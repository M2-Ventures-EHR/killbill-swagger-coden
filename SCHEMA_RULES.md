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


### HTTP Status

Every endpoint should return -- and have correct swagger annotation to return -- an http status in the `2xx` range upon success. Different statuses in that range are used to indicate different things:
* `200`: This is returned when the resource returns a response object
* `201`: This is returned when doing a `POST` and when the endpoint does not return a response object; instead we return an http `Location` header to specify how to fetch the created resource.
* `202`: This is returned when doing a `POST` and the processing is asynchronous
* `204`: This is returned when the resource does not return any object -- and no way to fetch the resource object through a `Location` header.

This is the current list of endpoint with default successful http status:

```
GET:
  200:
    - accounts:getAccountByKey
    - accounts:getAccounts
    - accounts:searchAccounts
    - accounts:getAccount
    - accounts:getAllCustomFields
    - accounts:getAllTags
    - accounts:getBlockingStates
    - accounts:getAccountBundles
    - accounts:getChildrenAccounts
    - accounts:getAccountCustomFields
    - accounts:getEmailNotificationsForAccount
    - accounts:getEmails
    - accounts:getInvoicePayments
    - accounts:getInvoicesForAccount
    - accounts:getOverdueAccount
    - accounts:getPaymentMethodsForAccount
    - accounts:getPaymentsForAccount
    - accounts:getAccountTags
    - accounts:getAccountTimeline
    - admin:getQueueEntries
    - bundles:getBundleByKey
    - bundles:getBundles
    - bundles:searchBundles
    - bundles:getBundle
    - bundles:getBundleCustomFields
    - bundles:getBundleTags
    - catalog:getCatalogJson
    - catalog:getAvailableAddons
    - catalog:getAvailableBasePlans
    - catalog:getPhaseForSubscriptionAndDate
    - catalog:getPlanForSubscriptionAndDate
    - catalog:getPriceListForSubscriptionAndDate
    - catalog:getProductForSubscriptionAndDate
    - catalog:getCatalogVersions
    - catalog:getCatalogXml
    - credits:getCredit
    - customFields:getCustomFields
    - customFields:searchCustomFields
    - export:exportDataForAccount
    - invoiceItems:getInvoiceItemCustomFields
    - invoiceItems:getInvoiceItemTags
    - invoicePayments:getInvoicePayment
    - invoicePayments:getInvoicePaymentCustomFields
    - invoicePayments:getInvoicePaymentTags
    - invoices:getCatalogTranslation
    - invoices:getInvoiceMPTemplate
    - invoices:getInvoices
    - invoices:searchInvoices
    - invoices:getInvoiceTemplate
    - invoices:getInvoiceTranslation
    - invoices:getInvoice
    - invoices:getInvoiceCustomFields
    - invoices:getInvoiceAsHTML
    - invoices:getPaymentsForInvoice
    - invoices:getInvoiceTags
    - invoices:getInvoiceByNumber
    - nodesInfo:getNodesInfo
    - overdue:getOverdueConfigJson
    - overdue:getOverdueConfigXml
    - paymentMethods:getPaymentMethodByKey
    - paymentMethods:getPaymentMethods
    - paymentMethods:searchPaymentMethods
    - paymentMethods:getPaymentMethod
    - paymentMethods:getPaymentMethodCustomFields
    - paymentTransactions:getPaymentByTransactionId
    - paymentTransactions:getTransactionCustomFields
    - paymentTransactions:getTransactionTags
    - payments:getPaymentByExternalKey
    - payments:getPayments
    - payments:searchPayments
    - payments:getPayment
    - payments:getPaymentCustomFields
    - payments:getPaymentTags
    - pluginsInfo:getPluginsInfo
    - security:getCurrentUserPermissions
    - security:getRoleDefinition
    - security:getCurrentUserSubject
    - security:getUserRoles
    - subscriptions:getSubscription
    - subscriptions:getSubscriptionCustomFields
    - subscriptions:getSubscriptionTags
    - tagDefinitions:getTagDefinitions
    - tagDefinitions:getTagDefinition
    - tags:getTags
    - tags:searchTags
    - tenants:getTenantByApiKey
    - tenants:getPushNotificationCallbacks
    - tenants:getPerTenantConfiguration
    - tenants:getAllPluginConfiguration
    - tenants:getPluginConfiguration
    - tenants:getPluginPaymentStateMachineConfig
    - tenants:getUserKeyValue
    - tenants:getTenant
    - usages:getAllUsage
    - usages:getUsage

POST:
  201:
    - accounts:createAccount
    - accounts:processPaymentByExternalKey
    - accounts:addAccountBlockingState
    - accounts:createAccountCustomFields
    - accounts:addEmail
    - accounts:createPaymentMethod
    - accounts:processPayment
    - accounts:createAccountTags
    - bundles:transferBundle
    - bundles:addBundleBlockingState
    - bundles:createBundleCustomFields
    - bundles:createBundleTags
    - catalog:addSimplePlan
    - catalog:uploadCatalogXml
    - credits:createCredit
    - invoiceItems:createInvoiceItemCustomFields
    - invoiceItems:createInvoiceItemTags
    - invoicePayments:createChargebackReversal
    - invoicePayments:createChargeback
    - invoicePayments:createInvoicePaymentCustomFields
    - invoicePayments:createRefundWithAdjustments
    - invoicePayments:createInvoicePaymentTags
    - invoices:createFutureInvoice
    - invoices:uploadCatalogTranslation
    - invoices:createExternalCharges
    - invoices:createMigrationInvoice
    - invoices:uploadInvoiceTemplate
    - invoices:uploadInvoiceTranslation
    - invoices:adjustInvoiceItem
    - invoices:createInvoiceCustomFields
    - invoices:createInstantPayment
    - invoices:createInvoiceTags
    - overdue:uploadOverdueConfigJson
    - overdue:uploadOverdueConfigXml
    - paymentMethods:createPaymentMethodCustomFields
    - paymentTransactions:notifyStateChanged
    - paymentTransactions:createTransactionCustomFields
    - paymentTransactions:createTransactionTags
    - payments:captureAuthorizationByExternalKey
    - payments:chargebackReversalPaymentByExternalKey
    - payments:chargebackPaymentByExternalKey
    - payments:createComboPayment
    - payments:refundPaymentByExternalKey
    - payments:captureAuthorization
    - payments:chargebackReversalPayment
    - payments:chargebackPayment
    - payments:createPaymentCustomFields
    - payments:refundPayment
    - payments:createPaymentTags
    - security:addRoleDefinition
    - security:addUserRoles
    - subscriptions:createSubscription
    - subscriptions:createSubscriptionWithAddOns
    - subscriptions:createSubscriptionsWithAddOns
    - subscriptions:addSubscriptionBlockingState
    - subscriptions:createSubscriptionCustomFields
    - subscriptions:createSubscriptionTags
    - tagDefinitions:createTagDefinition
    - tenants:createTenant
    - tenants:registerPushNotificationCallback
    - tenants:uploadPerTenantConfiguration
    - tenants:uploadPluginConfiguration
    - tenants:uploadPluginPaymentStateMachineConfig
    - tenants:insertUserKeyValue
  204:
    - accounts:payAllInvoices
  200:
    - admin:triggerInvoiceGenerationForParkedAccounts
    - invoices:generateDryRunInvoice
    - invoices:uploadInvoiceMPTemplate
    - paymentGateways:buildComboFormDescriptor
    - paymentGateways:buildFormDescriptor
    - paymentGateways:processNotification
    - usages:recordUsage
  202:
    - nodesInfo:triggerNodeCommand

PUT:
  204:
    - accounts:updateAccount
    - accounts:rebalanceExistingCBAOnAccount
    - accounts:modifyAccountCustomFields
    - accounts:setEmailNotificationsForAccount
    - accounts:refreshPaymentMethods
    - accounts:setDefaultPaymentMethod
    - accounts:transferChildCreditToParent
    - admin:putInRotation
    - admin:updatePaymentTransactionState
    - bundles:modifyBundleCustomFields
    - bundles:pauseBundle
    - bundles:renameExternalKey
    - bundles:resumeBundle
    - invoiceItems:modifyInvoiceItemCustomFields
    - invoicePayments:completeInvoicePaymentTransaction
    - invoicePayments:modifyInvoicePaymentCustomFields
    - invoices:commitInvoice
    - invoices:modifyInvoiceCustomFields
    - invoices:voidInvoice
    - paymentMethods:modifyPaymentMethodCustomFields
    - paymentTransactions:modifyTransactionCustomFields
    - payments:completeTransactionByExternalKey
    - payments:completeTransaction
    - payments:modifyPaymentCustomFields
    - security:updateRoleDefinition
    - security:updateUserPassword
    - security:updateUserRoles
    - subscriptions:changeSubscriptionPlan
    - subscriptions:updateSubscriptionBCD
    - subscriptions:modifySubscriptionCustomFields
    - subscriptions:uncancelSubscriptionPlan
    - subscriptions:undoChangeSubscriptionPlan

DELETE:
  204:
    - accounts:closeAccount
    - accounts:deleteAccountCustomFields
    - accounts:removeEmail
    - accounts:deleteAccountTags
    - admin:invalidatesCache
    - admin:invalidatesCacheByAccount
    - admin:invalidatesCacheByTenant
    - admin:putOutOfRotation
    - bundles:deleteBundleCustomFields
    - bundles:deleteBundleTags
    - catalog:deleteCatalog
    - invoiceItems:deleteInvoiceItemCustomFields
    - invoiceItems:deleteInvoiceItemTags
    - invoicePayments:deleteInvoicePaymentCustomFields
    - invoicePayments:deleteInvoicePaymentTags
    - invoices:deleteInvoiceCustomFields
    - invoices:deleteInvoiceTags
    - invoices:deleteCBA
    - paymentMethods:deletePaymentMethod
    - paymentMethods:deletePaymentMethodCustomFields
    - paymentTransactions:deleteTransactionCustomFields
    - paymentTransactions:deleteTransactionTags
    - payments:voidPaymentByExternalKey
    - payments:cancelScheduledPaymentTransactionByExternalKey
    - payments:voidPayment
    - payments:deletePaymentCustomFields
    - payments:deletePaymentTags
    - payments:cancelScheduledPaymentTransactionById
    - security:invalidateUser
    - subscriptions:cancelSubscriptionPlan
    - subscriptions:deleteSubscriptionCustomFields
    - subscriptions:deleteSubscriptionTags
    - tagDefinitions:deleteTagDefinition
    - tenants:deletePushNotificationCallbacks
    - tenants:deletePerTenantConfiguration
    - tenants:deletePluginConfiguration
    - tenants:deletePluginPaymentStateMachineConfig
    - tenants:deleteUserKeyValue
```
