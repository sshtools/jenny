<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>
--app-content package/target/bootstrap
--app-content package/target/app
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini
--java-options -Djenny.logging=lib/runtime/conf/logging.properties

<#elseif osName?upper_case?contains("MAC")>
--app-content package/target/bootstrap
--app-content package/target/app
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini
--java-options -Djenny.logging=lib/runtime/conf/logging.properties

<#elseif osName?upper_case?contains("LINUX")>
--app-content package/target/bootstrap
--app-content package/target/app
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini
--java-options -Djenny.logging=lib/runtime/conf/logging.properties

<#else>
--app-content package/target/bootstrap
--app-content package/target/app
--add-launcher jenny=package/src/main/launchers/jenny.properties
--arguments lib/runtime/conf/layers.ini
--java-options -Djenny.logging=lib/runtime/conf/logging.properties

</#if>