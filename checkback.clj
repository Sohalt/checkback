#!/usr/bin/env bb
(ns checkback
  (:require [babashka.process :as p]
            [babashka.http-client :as h]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import (java.time Instant)))

(def github-token (System/getenv "GITHUB_TOKEN"))

(defn github-api-req [url]
  (let [{:keys [status body]} (h/get (str "https://api.github.com" url) {:headers (cond-> {"Accept" "application/vnd.github.v3+json"}
                                                                                    github-token (assoc "Authorization" (str "Bearer " github-token)))
                                                                         :throw false})]
    (when (= 200 status)
      (json/parse-string body keyword))))

(defn checkback-lines []
  (-> (p/shell {:out :string
                :continue true} "rg" "--with-filename" "--line-number" "--only-matching" "--replace" "$2@$3@$4@$5@$6" "CHECKBACK: (https?://)?github.com/([^/]+)/([^/]+)/(issues|pulls|tags|releases)/?(\\d+)? (\".+\")?")
      :out
      str/split-lines))

(defn parse-line [line]
  (let [[_ file line match] (re-matches #"([^:]+):([^:]+):(.*)" line)
        [owner repo type num regex] (str/split match #"@")]
    {:file file
     :line line
     :owner owner
     :repo repo
     :type type
     :num num
     :regex regex}))

(defn date-for-line [{:keys [file line]}]
  (let [blame (:out (p/sh ["git" "blame" (format "-L%s,%s" line line) "-p" file]))
        [_ time] (re-find #"author-time (\d+)" blame)]
    (some-> time
            parse-long
            Instant/ofEpochSecond)))

(defn issue-has-update? [{:keys [owner repo num regex after]}]
  (some->> (github-api-req (str "/repos/" owner "/" repo "/issues/" num "/timeline"))
           (filter (fn [{:keys [updated_at]}] (> (Instant/parse updated_at) after)))
           (some (fn [{:keys [body]}] (when (re-matches regex body) body)))))

(defn pr-has-update? [{:keys [owner repo num regex after]}]
  (some->> (github-api-req (str "/repos/" owner "/" repo "/pulls/" num "/timeline"))
           (filter (fn [{:keys [updated_at]}] (> (Instant/parse updated_at) after)))
           (some (fn [{:keys [body]}] (when (re-matches regex body) body)))))

(defn issue-closed? [{:keys [owner repo num]}]
  (some-> (github-api-req (str "/repos/" owner "/" repo "/issues/" num))
      :state
      (= "closed")))

(defn pr-closed? [{:keys [owner repo num]}]
  (some-> (github-api-req (str "/repos/" owner "/" repo "/pulls/" num))
      :state
      (= "closed")))

(defn newer-release? [{:keys [owner repo regex after]}]
  (let [date-for-release (some->> (github-api-req (str "/repos/" owner "/" repo "/releases/"))
                                  (filter (fn [release-info] (re-matches regex (:name release-info))))
                                  first
                                  :published_at
                                  Instant/parse)]
    (when date-for-release
      (> date-for-release after))))

(defn newer-tag? [{:keys [owner repo regex after]}]
  (let [commit-for-tag (some->> (github-api-req (str "/repos/" owner "/" repo "/tags"))
                                (filter (fn [tag-info] (re-matches regex (:name tag-info))))
                                first
                                :commit
                                :sha)
        date-for-tag (some->> commit-for-tag
                         (str "/repos/" owner "/" repo "/commits/")
                         (github-api-req)
                         :commit
                         :committer
                         :date
                         Instant/parse)]
    (when date-for-tag
      (> date-for-tag after))))

(defn checkback []
  (doseq [line (checkback-lines)]
    (let [{:as line :keys [type regex]} (parse-line line)
          line (assoc line :after (date-for-line line))]
      (when (case type
         "issues" (if regex
                    (issue-has-update? line)
                    (issue-closed? line))
         "pulls" (if regex
                   (pr-has-update? line)
                   (pr-closed? line))
         "releases" (newer-release? line)
         "tags" (newer-tag? line))
        (println "updates for " line)))))

(when (= *file* (System/getProperty "babashka.file"))
  (checkback))
