# CHECKBACK

Searches for comments to Github issues and PRs and reports if there was any activity since the time the comment was added or if the issue/PR was closed.

## Example

```C

// Did the PR get closed?
//CHECKBACK: https://github.com/org/repo/pulls/123

// Did the PR get new comments matching the regex "fix.*" since this line got added?
//CHECKBACK: https://github.com/org/repo/pulls/123 "fix.*"

// Did the issue get closed?
//CHECKBACK: https://github.com/org/repo/issues/123

// Did the issue get new comments matching the regex "fix.*" since this line got added?
//CHECKBACK: https://github.com/org/repo/issues/123 "fix.*"

// Is there a new tag since this line got added?
//CHECKBACK: https://github.com/org/repo/tags

// Is there a new tag matching regex "v.*" since this line got added?
//CHECKBACK: https://github.com/org/repo/tags "v.*"

// Is there a new release since this line got added?
//CHECKBACK: https://github.com/org/repo/releases

// Is there a new release matching the regex "v.*" since this line got added?
//CHECKBACK: https://github.com/org/repo/releases "v.*"

```

Configure `GITHUB_TOKEN` if you run into rate limits.

## Credits

Inspired by https://github.com/ogham/checkback
