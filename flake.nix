{
  inputs.wbba.url = "github:Sohalt/write-babashka-application";
  inputs.flake-utils.url = "github:numtide/flake-utils";
  outputs = {
    self,
    nixpkgs,
    flake-utils,
    wbba,
    ...
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {
        inherit system;
        overlays = [
          wbba.overlays.default
          self.overlays.default
        ];
      };
    in {
      packages = rec {
        inherit (pkgs) checkback;
        default = checkback;
      };
    })
    // {
      overlays.default = nixpkgs.lib.composeExtensions wbba.overlays.default (final: prev: {
        checkback = final.callPackage ./package.nix {};
      });
    };
}
