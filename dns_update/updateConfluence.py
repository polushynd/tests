#!/usr/bin/python
from BeautifulSoup import BeautifulSoup
import requests
import json
import re, os


# Confluence credentials
url = "http://confluence:8090/rest/api/content"
articleID = '1018575'
urltoarticle = url + '/' + articleID
data_file = '/opt/json'
# username = "builduser"
# password = ""
# stringToEncode = username + ":" + password
# encodedString = base64.b64encode(stringToEncode)
encodedString = ''
headers = {'Authentication': 'Basic ' + encodedString, 'Content-type': 'application/json', 'Accept': 'application/json',
           'X-Atlassian-Token': 'no-check'}

# Docker Nexus
apiaddress = 'http://nexus.ixiasoft.local:2375'
response = requests.get("{}/containers/json".format(apiaddress))
nodeNumber = len(json.loads(response.text))

def parsemountpoints(r):
    temp = ''
    if (r != []):
        len_r = len(r)
        for i in range(0, len_r):
            temp = temp + "<h6 style=\"text-align: left;\">Src:{}</h6><h6 style=\"text-align: left;\">Dst:{} </h6>".format(
                r[i]['Source'], r[i]['Destination'])
    else:
        temp = 'no mount point'
    return temp


def printValues(finalContainerName, dockerImage, ip, dbName, mPoint):
    print finalContainerName
    print dockerImage
    print ip
    print dbName
    print mPoint
    print "-------------------------"

def addComment(finalContainerName, bol):
    if bol == False:
        requestForNumber = requests.get(urltoarticle + "?expand=space,body.view,version,container")
        text = json.loads(requestForNumber.text)
        html = text['body']['view']['value']
        parsed_html = BeautifulSoup(html)
        dict2 = {}
        if os.path.exists(data_file):
            with open(data_file) as f:
                dict1 = json.load(f)
                f.close()
        else:
            dict1 = {}
        element = parsed_html.findAll('tr')
        for i in element:
            try:
                if i.findAll("td"):
                    key = i.findAll("td")[0].text
                    value = i.findAll("td")[-1].contents[0]
                    dict2[str(key)] = str(value)
                else:
                    print "can't find!"
            except:
                print "can't find1!"

        dict0 = dict(dict1.items() + dict2.items())
        with open(data_file, 'w') as f:
            try:
                json.dump(dict0, f)
            except Exception:
                 print Exception
    with open(data_file) as f:
        dict0 = json.load(f)
        return dict0[finalContainerName]

def generate_table():
    rows = ''
    count = 0
    try:
        for i in range(0, nodeNumber):
            containername = json.loads(response.text)[i]['Names'][0].replace('/', '') + '.dkr.ixiasoft.local'
            tomcatPort = getTomcatPort(containername.split('.')[0])
            dockerImage = json.loads(response.text)[i]['Image']
            finalContainerName = toahref(containername, dockerImage, tomcatPort)
            if i == 0:
                comment = addComment(containername, False)
                count = count + 1
            else:
                comment = addComment(containername, True)
            ip = json.loads(response.text)[i]['NetworkSettings']['Networks']['bridge']['IPAddress']
            mPoint = parsemountpoints(json.loads(response.text)[i]['Mounts'])
            dbName = dbname(containername)
            # printValues(finalContainerName, dockerImage, ip, dbName, mPoint)
            rows = rows + '<tr><td class="confluenceTd">{}</td><td class="confluenceTd">{}</td><td colspan="1" ' \
                          'class="confluenceTd">{}</td><td style="text-align: center;" colspan="1" class="confluenceTd"><span style="color: rgb(51,102,255);">{}</span></td>' \
                          '<td style="text-align: center;" colspan="1" class="confluenceTd">{}</td><td style="text-align: center;" colspan="1" class="confluenceTd">{}</td></tr>'.format(
                finalContainerName, dockerImage, ip, dbName, mPoint, comment)
    except:
        pass
    return rows


def getConfluenceArticleNumber():
    currentNumber = 0
    try:
        requestForNumber = requests.get(urltoarticle, headers=headers)
        currentNumber = json.loads(requestForNumber.text)['version']['number'] + 1
    except Exception as e:
        print "Error in getArticleNumber(): {}".format(e)
    return currentNumber


def getTomcatPort(containername):
    tomcatport = 'no tomcatport'
    try:
        params = {"Resource": "/opt/tomcat7/conf/server.xml"}
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.post("{}/containers/{}/copy".format(apiaddress, containername),
                                 data=json.dumps(params), headers=headers)
        tomcatport = response.text
        for line in tomcatport.splitlines():
            if '<Connector port="' in line:
                tomcatport = line.split('<Connector port="')[1].split("\"")[0]
                break
    except:
        pass
    if (re.search(r'\d{4}', tomcatport)):
        tomcatport = tomcatport
    else:
        tomcatport = 0
    return tomcatport


def toahref(containername, image, tport):
    if tport > 0:
        print image
        if 'ditacms' in image:
            result = "<a href=\"http://{}:{}/ditacms\">{}</a>".format(containername, tport, containername)
        else:
            result = "<a href=\"http://{}:{}/webauthorx\">{}</a>".format(containername, tport, containername)
    else:
        result = containername
    return result


def dbname(containername):
    db = 'no db'
    try:
        params = {"Resource": "/opt/glassfish4/glassfish/domains/domain1/config/CMSAppServer.config"}
        headers = {'Content-type': 'application/json', 'Accept': 'text/plain'}
        response = requests.post("{}/containers/{}/copy".format(apiaddress, containername.split('.')[0]),
                                 data=json.dumps(params), headers=headers)
        db = response.text.split('docbase="')[1].split('"/')[0]
    except:
        pass
    return db


def updateArticle(currentNumber, tablecontent):
    tables = '''<table class="confluenceTable"><colgroup><col /><col /><col /><col /></colgroup><tbody><tr><th class="confluenceTh">VM</th><th class="confluenceTh">Web Author Version</th><th colspan="1" class="confluenceTh"><span>IP address</span></th><th colspan="1" class="confluenceTh">DB on WAX1</th><th colspan="1" class="confluenceTh">Mount point</th><th colspan="1" class="confluenceTh">Comment</th></tr>''' + tablecontent + '''</tbody></table>'''

    content = {"version": {"number": currentNumber}, "title": "Liste des VMs avec Web Author", "type": "page",
               "space": {"key": "DEV"},
               "body": {"storage": {"value": tables, "representation": "storage"}}}
    try:
        updateContent = requests.put(urltoarticle, data=json.dumps(content), headers=headers)
        print updateContent.text
    except Exception as e:
        print "Error in updateArticle(): {}".format(e)


# Get current Confluence ID number
count = 0
currentNumber = getConfluenceArticleNumber()
# Generate table content
tablecontent = generate_table()
# Push content in to Confluence
updateArticle(currentNumber, tablecontent)
