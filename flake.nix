{
  description = "baboon build environment";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/24.05";

  inputs.flake-utils.url = "github:numtide/flake-utils";

  inputs.sbt.url = "github:zaninime/sbt-derivation";
  inputs.sbt.inputs.nixpkgs.follows = "nixpkgs";

  outputs =
    { self
    , nixpkgs
    , flake-utils
    , sbt
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        packages = rec {
          idealingua-v1 = sbt.lib.mkSbtDerivation {
            pkgs = pkgs;
            version = "1.3.17";
            pname = "idealingua-v1";
            src = ./.;
            depsSha256 = "sha256-d9sTBvQbLwXH9aqy6N+a79jD8SIn9+/87KWZ3gXXW/I=";
            nativeBuildInputs = with pkgs; [
              coursier
              libarchive
            ];
            depsWarmupCommand = ''
              ./sbtgen.sc
              sbt "++2.13 clean" "++2.13 compile"
            '';
            buildPhase = ''
              ./sbtgen.sc
              sbt "++2.13 clean" "++2.13 Universal/packageBin"
            '';
            installPhase = ''
              mkdir -p $out
              bsdtar -xf ./idealingua-v1/idealingua-v1-compiler/target/universal/idealingua-v1-compiler-*.zip --strip-components 1 -C $out/
            '';
          };
          default = idealingua-v1;
        };

        devShells.default = pkgs.mkShell {
          nativeBuildInputs = with pkgs.buildPackages; [
            ncurses

            graalvm-ce
            coursier
            pkgs.buildPackages.sbt

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

            coreutils
            shellspec
            jq
            nix
            gitMinimal

          ];
        };
      }
    );
}
