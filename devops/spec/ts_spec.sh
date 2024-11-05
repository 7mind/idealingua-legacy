Describe 'Typescript transpiler'
  Include devops/lib/builders.sh

  Parameters:dynamic
    while read line; do
      %data "$line"
    done <<< "$(find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d)"
  End

  It "builds Typescript Yarn project in $1"
    When call test_ts_yarn_prj "$1"
    The status should be success
    The output should match pattern '*'
  End

  It "builds Typescript Plain project in $1"
    When call test_ts_plain_prj "$1"
    The status should be success
    The output should match pattern '*'
  End
End
