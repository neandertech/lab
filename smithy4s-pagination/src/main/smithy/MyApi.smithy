$version: "2"

namespace com.example.hello

use alloy#simpleRestJson

@simpleRestJson
service HelloWorldService {
    version: "1.0.0"
    operations: [Hello, Pages]
}

@http(method: "POST", uri: "/hello/{name}", code: 200)
operation Hello {
    input: Person
    output: Greeting
}

@length(min: 1)
string BookID

@readonly
@paginated(inputToken: "page", outputToken: "nextPage")
@http(method: "GET", uri: "/pages/{book}", code: 200)
operation Pages {
    input := {
        @httpLabel
        @required
        book: BookID
        @httpQuery("page")
        page: String
    }
    output := {
        nextPage: String
        @required
        data: Strings
    }
}

list Strings {
    member: String
}

structure Person {
    @httpLabel
    @required
    name: String
    @httpQuery("town")
    town: String
}

structure Greeting {
    @required
    message: String
}
