<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>
--app-content package/target/jenny-logging
--app-content package/target/jenny-api
--app-content package/target/jenny-web
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini

<#elseif osName?upper_case?contains("MAC")>
--app-content package/target/jenny-logging
--app-content package/target/jenny-api
--app-content package/target/jenny-web
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini

<#elseif osName?upper_case?contains("LINUX")>
--app-content package/target/jenny-logging
--app-content package/target/jenny-api
--app-content package/target/jenny-web
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini

<#else>
--app-content package/target/jenny-logging
--app-content package/target/jenny-api
--app-content package/target/jenny-web
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini

</#if>