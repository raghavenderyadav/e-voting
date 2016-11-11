#!/usr/bin/env bash
     docker pull hyperledger/fabric-peer
     docker pull hyperledger/fabric-membersrvc
     docker pull hyperledger/fabric-ccenv:x86_64-0.6.1-preview
     docker tag hyperledger/fabric-ccenv:x86_64-0.6.1-preview hyperledger/fabric-ccenv
     docker pull hyperledger/fabric-baseimage:x86_64-0.2.0
     docker tag hyperledger/fabric-baseimage:x86_64-0.2.0 hyperledger/fabric-baseimage
