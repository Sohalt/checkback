{
  writeBabashkaApplication,
  git,
  ripgrep,
}:
writeBabashkaApplication {
  name = "checkback";
  runtimeInputs = [git ripgrep];
  text = builtins.readFile ./checkback.clj;
}
