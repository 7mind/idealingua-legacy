Describe 'Protobuf transpiler'
  Include devops/lib/builders.sh

  Parameters:dynamic
    %data ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs/main-tests
    %data ./idealingua-v1/idealingua-v1-test-defs/src/main/resources/defs/overlays
  End

  It "builds Protobuf project in $1"
    When call test_pb_prj "$1"
    The status should be success
    The output should match pattern '*'
    The stderr should match pattern '*'
  End
End
