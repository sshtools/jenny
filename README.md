# Jenny

A lightweight modular web application framework for Java based on [Bootlace](https://github.com/sshtools/bootlace), [tinytemplate(https://github.com/sshtools/tinytemplate) and [uHTTP](https://github.com/sshtools/uhttpd)
that aims to be fast, flexible and fun to use.

## Design

Jenny is based on just 3 of our own projects, each of which was written from the ground up to be
as simple as possible and avoid requiring large external 3rd party libraries and frameworks. 

 * [Bootlace](https://github.com/sshtools/bootlace). A JPMS based plugin framework.
 * [uHTTPD](https://github.com/sshtools/uhttpd). An embeddable HTTP/HTTPs server.
 * [tinytemplate](https://github.com/sshtools/tinytemplate). A simple but flexible string templating library .

## Features

 * Dynamic loading of extensions, add and remove functionality while the server is running 
 * Plain and pure Java. No reflection, dependency injection, annotation scanning, AOP or other weirdness.

## Extensions

Using Bootlace's module layers, Jenny is made up of extensions. There are 2 classes of extensions.

### Framework

Framework extensions provide some common piece of functionality that may be shared and used
by multiple dependent plugins. This saves individual plugins including their own copies of
such libraries, the framework is guaranteed to provide them.  

 * [dbus](framworks/dbus/README.md). Provides access to [DBus-Java](https://github.com/hypfvieh/dbus-java), used to access and provide services over [D-Bus](https://www.freedesktop.org/wiki/Software/dbus).
 * [logging](framworks/logging/README.md). Adds the [SLF4J](http://slf4j.org/) logging framework.
 * [jna](framworks/jna/README.md). Allows plugins to access other native libraries.

### Plugins

Plugins Jenny comes with a suite a plugins to help you build your application.

 * [auth-linux](auth/linux/README.md). Linux PAM authentication.
 * [bootstrap5](plugins/bootstrap5/README.md). Adds [Bootstrap 5](https://getbootstrap.com/docs/5.0/getting-started/introduction/) stylesheets and scripts.
 * [bootswatch](plugins/bootswatch/README.md). Provides open source [Bootswatch](https://bootswatch.com/) themes for Bootstrap.
 * [config](plugins/config/README.md). Simple INI file based configuration facilities for applications to use.
 * [io](plugins/io/README.md). Websocket based JSON messaging framework. 
 * [plugin-manager](plugins/plugin-manager/README.md). Install or update plugins.
 * [toast](plugins/toast/README.md). Popup "Toast" messages in your web applications.

