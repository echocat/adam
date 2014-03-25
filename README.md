echocat Adam
============

Synopsis
--------
Addon which enhances all user profiles of confluence. It also adds an advanced people directory. The whole addon is configurable by means of an XML, can be localized, supports Velocity templates and supports view and edit restrictions.

Status
------
This addon is currently in beta phase and under heavy development. [Please help us to improve and report issues.](https://github.com/echocat/adam/issues)

Installation
------------
You have several options:

1. You can install it direct over the addon manager of confluence.
2. You can download it from [Marketplace](https://marketplace.atlassian.com/plugins/org.echocat.adam) and install it manually.

Configuration
-------------

1. Go to ``<you base confluence url>/plugins/org.echocat.adam/administration.action``. Example ``http://wiki.foobar.com/plugins/org.echocat.adam/administration.action``
2. Edit the configuration XML.

Sorry for now there is no documentation about the configuration XML and it could be changed because this is currently a beta. But you could refer the [Schema XSD](https://raw.githubusercontent.com/echocat/adam/master/src/main/resources/org/echocat/adam/schemas/configuration.xsd). The configuration will be validated against it before save. If there is an error you will notified about the problem and the line where the problem is located.

Links
-----
* [Marketplace listing](https://marketplace.atlassian.com/plugins/org.echocat.adam)
* [Project page](https://github.com/echocat/adam)
* [Issue tracker](https://github.com/echocat/adam/issues)
* [Wiki](https://github.com/echocat/adam/wiki)
* [echocat](https://echocat.org)
* [echocat on GitHub](https://github.com/echocat)

License
-------
echocat Adam is licensed under [GNU Lesser General Public License 3.0](https://www.gnu.org/licenses/lgpl-3.0.txt).