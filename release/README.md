# Prerequisites
- GPG should be available on command line
```
gpg --version
gpg (GnuPG) 2.4.3
...
```

- identity key should be available
```
gpg --list-secret-keys
```

# Trust Signing Key Used to Sign Distributables
```
cd ~/Downloads/NIFI/2.0.0-M1
wget https://dist.apache.org/repos/dist/dev/nifi/KEYS
gpg --import KEYS
gpg --sign-key B18613173237750DBAB0A2B929B6A52D2AAE8DBA
gpg --list-signatures B18613173237750DBAB0A2B929B6A52D2AAE8DBA
```

# Verify Distributables
```
cd ~/scm/com.github/greyp9/nifi-tidbits/release
./verify-distributables.sh
```
