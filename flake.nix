{
  description = "baboon build environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/24.05";

  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs =
    { self
    , nixpkgs
    , flake-utils
    ,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = with pkgs.buildPackages; [
            ncurses

            graalvm-ce
            coursier
            sbt

            dotnet-sdk_6
            mono
            msbuild
            dotnetPackages.NUnitConsole
            dotnetPackages.Nuget

            protobuf

            nodejs
            nodePackages.npm
            typescript
            yarn
          ];
        };
      }
    );
}
