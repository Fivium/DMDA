Name:           dmda
Version:        1.6
Release:        1
Summary:        Database Mail Delivery Agent - A simple mail delivery agent designed to store email in a database backend

License:        BSD
URL:            fivium.co.uk
Source0:        fivium.co.uk/dmda


%description
Database Mail Delivery Agent
==================================================
DMDA is a Java application used to store email in a database. It is designed to sit behind a Mail Transfer Agent such as postfix and will store emails in the configured databases based on the recipient's email address's domain.

This application was developed as an alternative for Apache James. James is a large POP3/IMAP server that does a lot of things including storing email in the database however it is very heavy weight.

The main goals of this tool were:
- Reliability: The server uses HikariCP to manage the database connection pool and will reconnect should the connection drop
- Data integrity: The tool will either reject an email or store it. There will be no dropped emails.
- Light weight: The tool is small, fast and easy to configure.

%prep
rm -rf %_sourcedir/dmda
cd  %_builddir
mkdir %_sourcedir/dmda
tar -xzf %_sourcedir/dmda.tar.gz


%build


%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt/dmda
mkdir -p %{buildroot}/etc/init.d
cp -p  %_builddir/* %{buildroot}/opt/dmda
mv %{buildroot}/opt/dmda/dmdad %{buildroot}/etc/init.d

%clean
rm -rf %{buildroot}


%files
%defattr(-,root,root,-)
/etc/init.d/dmdad
/opt/dmda/dmda.jar
/opt/dmda/LICENSE
/opt/dmda/config.xml.sample


%post
chmod u+x /etc/init.d/dmdad


%changelog
* Fri May 07 2021 James Barnett <james.barnett@fivium.co.uk> - 1.6
- Anti-virus configurable via environment variables
* Tue Jul 04 2017 James Barnett <james.barnett@fivium.co.uk> - 1.5
- Added application health checks
* Tue Jul 26 2016 Jonathan Poole <jon.poole@fivium.co.uk> - 1.4
- Attachment stripping, domain wildcards, configuration enhancements and bug fixes
* Tue Mar 08 2016 Nick Palmer <nick.palmer-mills@fivium.co.uk> - 1.3
- Added a /smtp_config/anti_virus/timeout_ms config property so the timeout on the response from ClamAV can be defined.
- General code tidy
* Thu Mar 03 2016 Chris Cameron-Mills <chris.cameron-mills@fivium.co.uk> - 1.2
- Case insensitive domain matching
* Thu Jul 07 2015 Jonathan Poole <jon.poole@fivium.co.uk> - 1.0
- Initial version of the package
