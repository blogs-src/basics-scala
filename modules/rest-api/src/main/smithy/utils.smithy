$version: "2"

namespace smithy_rest.utils

@trait
structure authToken{
  @required
  roles: StringList
}

@trait
structure authSign {
}

list StringList{
    member: String
}
