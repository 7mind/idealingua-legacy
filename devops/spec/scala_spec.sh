Describe 'Scala transpiler'
  Include devops/lib/builders.sh

  setup() {
    export classpath="$(TERM=dumb sbt --batch --error "$VERSION_COMMAND ; export idealingua-v1-compiler/runtime:fullClasspath")"
  }

  BeforeAll 'setup'

  Parameters:dynamic
    while read line; do
      %data "$line"
    done <<< "$(find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs -maxdepth 1 -mindepth 1  -type d)"
  End
  
  It "builds Scala SBT project in $1"
    When call test_scala_sbt_prj "$1"
    The status should be success
    The output should match pattern '*'
    The stderr should match pattern '*'
  End

  It "builds Scala Plain project in $1"
    When call test_scala_plain_prj "$1"
    The status should be success
    The output should match pattern '*'
    The stderr should match pattern '*'
  End
End


Describe 'Scala transpiler (scala-only case)'
  Include devops/lib/builders.sh

  setup() {
    export classpath="$(TERM=dumb sbt --batch --error "$VERSION_COMMAND ; export idealingua-v1-compiler/runtime:fullClasspath")"
  }

  BeforeAll 'setup'

  Parameters:dynamic
    while read line; do
      %data "$line"
    done <<< "$(find ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs-special -name 'scala-*' -maxdepth 1 -mindepth 1  -type d)"
  End

  It "builds Scala Plain project in $1"
    When call test_scala_plain_prj "$1"
    The status should be success
    The output should match pattern '*'
    The stderr should match pattern '*'
  End
End
