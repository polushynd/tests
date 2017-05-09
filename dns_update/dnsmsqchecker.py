#!/usr/bin/python
import requests
import json
import time
import sys
import logging
import subprocess

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s %(levelname)s %(message)s',
                    filename='/opt/dns_update.log',
                    filemode='w')

prefixDNS = '.dkr.ixiasoft.local'
propFile = '/opt/dnsmasq.conf'
sleepTime = 120
dockerHostsDNS = 'http://registry.ixiasoft.local:2375'
dockerHosts = [dockerHostsDNS+'/containers/json', 'http://somename/containers/json']
allHostsIPnew = ''


def connectToAPI(dockerH):
    response = requests.get(dockerH)
    return json.loads(response.text)

# json request, respose
def dockerApiResponse(dockerH):
    hostdns = ''
    try:
        jsonContainerResponse = connectToAPI(dockerH)
        for i in range(0, len(jsonContainerResponse)):
            try:
                hostdns +='address={}{}/{}\n'.format(str(jsonContainerResponse[i]['Names'][0]), prefixDNS, jsonContainerResponse[i]['NetworkSettings']['Networks']['bridge']['IPAddress'])
            except:
                pass
    except requests.exceptions.ConnectionError, detail:
        print "Caught a ConnectionError:", detail.message
        logging.info('Caught a ConnectionError!')
        pass
    return hostdns


def allHostsIP():
    allHosts = ''
    for i in dockerHosts:
        allHosts += dockerApiResponse(i)
    return allHosts


def readDnsConfigWithoutNodes():
    temp = ''
    with open(propFile) as f:
        for line in f:
            if '#explicitly' in line:
                temp = temp + line
                break
            else:
                temp = temp + line
    return temp


def writeToConfigFile():
    writeToFile = readDnsConfigWithoutNodes() + allHostsIP()
    f = open(propFile, 'w')
    f.write(writeToFile)
    f.close()


# HN with DNS container!
def containerDnsId():
    containerID = ''
    try:
        result = connectToAPI(dockerHostsDNS+"/containers/json")
        nodeNumber = len(result)
        for i in range(0, nodeNumber):
            if (str(result[i]['Names'][0])) == '/dnsmasq':
                containerID = result[i]['Id']
                break
        if(containerID == ''):
            logging.info("Can not find dnsmasq container!")
    except:
        logging.info("HN with DNS container!")
        sys.exit("HN with DNS container!")
    return containerID



def dnsContainerStopStart():
    container = containerDnsId()
    try:
        if containerDnsId != 'Can not find dnsmasq container!':
            postStop = dockerHostsDNS + "/containers/" + container + "/stop"
            r = requests.post(postStop)
            if r.status_code != 204:
                logging.info('Can not stop DNS container!')
                sys.exit("Can not stop DNS container!")
            else:
                postStart = dockerHostsDNS + "/containers/" + container + "/start"
                r = requests.post(postStart)
                if r.status_code != 204:
                    logging.info('Can not start DNS container !')
                    sys.exit("Can not start DNS container !")
                else:
                    logging.info("DNS container has been restarted!")
    except Exception as e:
        sys.exit("Error in dnsContainerStopStart: {}".format(e))


while True:
    allHostsIPold = allHostsIP()
    try:
        if (allHostsIPold == allHostsIPnew):
            logging.info('No changes in dns config.')
        else:
            allHostsIPnew = allHostsIPold
            logging.info('dns config will be updated.')
            writeToConfigFile()
            dnsContainerStopStart()
	    subprocess.call("/opt/updateConfluence.py", shell=True)
	time.sleep(sleepTime)
    except:
        pass
