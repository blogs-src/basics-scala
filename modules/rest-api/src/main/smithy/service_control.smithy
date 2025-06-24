$version: "2"

namespace smithy4s.service_control

use alloy#simpleRestJson

@simpleRestJson
service ControlService {
version: "1.0.0",
errors: [],
operations: [ReloadJWKS]
}

@http(method: "POST", uri: "/reload-jwks", code: 200)
operation ReloadJWKS {
    input: Unit,
    output: Unit
}
