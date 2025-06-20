$version: "2"

namespace smithy_rest.utils

@trait
structure authToken{
  @required
  roles: StringList
}

list StringList{
    member: String
}
