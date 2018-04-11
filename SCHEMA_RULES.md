# Swagger Generated API

Since we are relying on swagger svhema to generate our clients, it makes sense to have start from a valid schema and
also enforce some rules to ensure some consistency in the generated apis.

## Swagger Spec

The generated schema should respect the [swagger 2.0 specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md)

TODO: Run validation tool.

## Kill Bill Rules

The following rules are somewhat arbitrary and mostly in place to guarantee some uniformity in the auto-generated client libraries.

The [validate.rb](https://github.com/killbill/killbill-swagger-coden/blob/master/validate.rb) script can be used to verify the schema follows the rules below.

### Argument Ordering

1. Argument types should be orderd using the folowing: `path`, `body`, `query`, `header`
2. Query parameters, when present, should follow specific ordering: `controlPluginName`, `pluginProperty`, `audit`
3. Headers parameters, when present, should follow specific ordering: `X-Killbill-CreatedBy`, `X-Killbill-Reason`, `X-Killbill-Comment`, `X-Killbill-ApiKey`, `X-Killbill-ApiSecret`




