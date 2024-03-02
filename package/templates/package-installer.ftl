<#-- template to create the options file for the jpackage tool to create the installer -->
<#if osName?upper_case?contains("WIN")>
--type exe
--name jenny

<#elseif osName?upper_case?contains("MAC")>

--type dmg
--name jenny

<#elseif osName?upper_case?contains("LINUX")>
--type deb
--linux-package-name jenny
--name jenny

<#else>

--name jenny

</#if>