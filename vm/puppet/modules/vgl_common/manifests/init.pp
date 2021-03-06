# Installs common VL dependencies for Centos
# Depends on the stahnma/epel module and python_pip module

class vgl_common {

    # Install default packages
    package { ["wget", "subversion", "mercurial", "ftp", "bzip2", "elfutils", "ntp", "ntpdate", "gcc", "gcc-c++", "make", "openssh", "openssh-clients", "swig", "libpng-devel", "freetype-devel", "atlas", "atlas-devel", "libffi-devel"]: 
        ensure => installed,
        require => Class["epel"],
    }
    
    # Install default pip packages
    package {  ["numpy", "boto", "pyproj", "swift", "python-keystoneclient"]:
        ensure => installed,
        provider => "pip",
        require => Class["python_pip"],
    }  
    package {["scipy"]:
        ensure => installed,
        provider => "pip",
        require => [Class["python_pip"], Package["numpy"]],
    }

    
    
    # Install startup bootstrap
    $curl_cmd = "/usr/bin/curl"
    $bootstrapLocation = "/etc/rc.d/rc.local"
    exec { "get-bootstrap":
        before => File[$bootstrapLocation],
        command => "$curl_cmd -L https://raw.githubusercontent.com/AuScope/VEGL-Portal/master/vm/ec2-run-user-data.sh > $bootstrapLocation",
    }
    file { $bootstrapLocation: 
        ensure => present,
        mode => "a=rwx",
    }
}
