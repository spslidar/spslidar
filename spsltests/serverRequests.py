import requests
import json
import utils
from pprint import pprint
from requests_toolbelt import MultipartEncoder

"""
Class that defines the methods for making petitions to the server
"""
quiet = False
server = "http://localhost:8080/";

"""
Get all workspaces
"""
def getWorkspace():
    endpoint = server + "spslidar/workspaces"
    r = requests.get(endpoint)
    result = r.text

    lines = result.splitlines()
    for line in lines:
        jsonline = json.loads(line)
        pprint(jsonline)


"""
Get single workspace identified by its name
"""


def getWorkspaceByName(workspaceName):
    endpoint = server + "spslidar/workspaces/" + workspaceName
    r = requests.get(endpoint)
    showResults(r)


"""
Post new workspace
"""


def postWorkspace(workspace):
    endpoint = server + "spslidar/workspaces"
    r = requests.post(endpoint, json=workspace)
    showResults(r)


"""
Get all datasets from a workspace
"""


def getDatasetsFromWorkspace(workspaceName, coordinatesReqParam):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets"
    r = requests.get(endpoint)
    showResults(r)


"""
Gets a single dataset identified by its workspace and its own name
"""


def getDatasetByName(workspaceName, datasetName):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets/" + datasetName
    r = requests.get(endpoint)
    showResults(r)


"""
Posts new dataset
"""


def postDataset(workspaceName, dataset):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets"
    r = requests.post(endpoint, json=dataset)
    showResults(r)


"""
Get datablock
"""


def getDatablock(workspaceName, datasetName, id, coordinatesReqParam=None):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets/" + datasetName + "/datablocks/" + id
    if coordinatesReqParam is None:
        r = requests.get(endpoint)
    else:
        r = requests.get(endpoint, coordinatesReqParam)

    return r.text


"""
Get datablock file
"""


def getDatablockFile(workspaceName, datasetName, id, coordinatesReqParam):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets/" + datasetName + "/datablocks/" + id + "/data"
    r = requests.get(endpoint, params=coordinatesReqParam)
    #utils.writeFile(r, workspaceName, datasetName, id) #Write downloaded files if wanted


"""
Assign dataset 
"""
def putData(workspaceName, datasetName, dataset):
    endpoint = server + "spslidar/workspaces/" + workspaceName + "/datasets/" + datasetName + "/data"
    mp_encoder = MultipartEncoder(fields=dataset)
    print(mp_encoder.content_type)
    r = requests.put(endpoint, data=mp_encoder, headers={"Content-Type": mp_encoder.content_type})
    showResults(r)


"""
Generic method to print the statuds code recevied and the content
"""


def showResults(request):
    print("Status code returned: ", str(request.status_code))
    if quiet == False:
        try:
            jsondata = json.loads(request.text)
            pprint(jsondata)
        except:
            print(request.text)


"""
Reset database
"""


def resetDatabase():
    endpoint = server + "spslidar/database"
    r = requests.delete(endpoint)

    showResults(r)


"""
Get octree size
"""


def getOctreeSize(workspace, dataset):
    endpoint = server + "spslidar/workspaces/" + workspace + "/datasets/" + dataset + "/size"
    r = requests.get(endpoint)
    return r.text


"""
Get max depth
"""


def getOctreeMaxDepth(workspace, dataset):
    endpoint = server + "spslidar/workspaces/" + workspace + "/datasets/" + dataset + "/depth"
    r = requests.get(endpoint)
    return r.text


"""
Modify max depth defined for octrees
"""


def modifyMaxDepthOctree(size):
    endpoint = server + "spslidar/octree/" + str(size)



"""
Get database size
"""

def getDatabaseSize():
    endpoint = server + "spslidar/database"
    r = requests.get(endpoint)
    return r.text
