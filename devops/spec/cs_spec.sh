Describe 'CS transpiler'
  Include devops/lib/builders.sh

  setup() {
    export refsdir="$PWD/idealingua-v1/idealingua-v1-test-defs/src/main/resources/refs/csharp"
    export refs="$(find "$refsdir" -name "*.dll" -exec echo "/reference:{}" \; | xargs -0 | tr '\n' ' ')"
  }

  BeforeAll 'setup'

  Parameters:dynamic
    while read line; do
      %data "$line"
    done <<< "$(find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d)"
  End

  It "builds CS MSBuild project in $1"
    When call test_cs_msbuild_prj "$1"
    The status should be success
    The output should match pattern '*'
  End

  It "builds CS Nuget project in $1"
    When call test_cs_plain_prj "$1"
    The status should be success
    The output should match pattern '*'
  End
End
