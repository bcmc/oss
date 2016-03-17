#!/bin/bash

# Determine OS platform
UNAME=$(uname | tr "[:upper:]" "[:lower:]")
# If Linux, try to determine specific distribution
if [ "$UNAME" == "linux" ]; then
    # If available, use LSB to identify distribution
    if [ -f /etc/lsb-release -o -d /etc/lsb-release.d ]; then
        export DISTRO=$(lsb_release -i | cut -d: -f2 | sed s/'^\t'//)
    # Otherwise, use release info file
    else
        export DISTRO=$(ls -d /etc/[A-Za-z]*[_-][rv]e[lr]* | grep -v "lsb" | cut -d'/' -f3 | cut -d'-' -f1 | cut -d'_' -f1)
    fi
fi
# For everything else (or if above failed), just use generic identifier
[ "$DISTRO" == "" ] && export DISTRO=$UNAME
unset UNAME

# install Oracle JDK 8
if [ "$DISTRO" == "Ubuntu" ]; then
    apt-get install python-software-properties
    add-apt-repository ppa:webupd8team/java
    apt-get update
    apt-get install oracle-java8-installer
    apt-get install oracle-java8-set-default
fi

# install Advanced Intrusion Detection Environment (aide)
if [ "$DISTRO" == "Ubuntu" ]; then
    apt-get update
    apt-get install aide
else
    yum -y update
    yum -y install aide
fi

# update crontab to run aide
echo ""
echo "Configuring & initializing aide..."
crontab -l > /tmp/mycron;
if [ "$(grep aide /tmp/mycron)" == '' ]; then
    aide --init
    ln -s /var/lib/aide/aide.db.new.gz /var/lib/aide/aide.db.gz
    echo "05 4 * * * root /usr/sbin/aide --check" >> /tmp/mycron
    crontab /tmp/mycron
fi
rm -f /tmp/mycron

# install auditd
if [ "$DISTRO" == "Ubuntu" ]; then
    apt-get install auditd audispd-plugins
fi

# configure audit system
if [ "$(grep FLAREclient /etc/audit/audit.rules)" == '' ]; then
    echo ""
    echo "Configuring audit system..."
    echo "# These four lines have been added by FLAREclient install" >> /etc/audit/audit.rules
    echo "-w /sbin/insmod -p x -k modules" >> /etc/audit/audit.rules
    echo "-w /sbin/rmmod -p x -k modules" >> /etc/audit/audit.rules
    echo "-w /sbin/modprobe -p x -k modules" >> /etc/audit/audit.rules
    echo "-a always,exit -f arch=b64 -S init_module -S delete_module -k modules" >> /etc/audit/audit.rules
fi

# adjust permissions on boot.log
echo ""
echo "Adjusting permissions on boot.log"
chmod 0600 /var/log/boot.log

# override ctrl-alt-delete
echo ""
echo "Overriding control-alt-delete..."
echo -e "start on control-alt-delete\n\nexec /usr/bin/logger -p security.info \"Control-Alt-Delete pressed\"" > /etc/init/control-alt-delete.override

# add "monitored" email address to /etc/aliases
echo ""
echo "Adding 'monitored' email address to /etc/aliases..."
echo "root: dte-operations@cert.org" >> /etc/aliases
newaliases

echo "Done!"
