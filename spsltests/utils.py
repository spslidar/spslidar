from _datetime import datetime
import glob
import subprocess
import os

"""
Returns dictionary of the bounding box coordinates
"""


def loadDirectory(path):
    filesPaths = glob.glob(path + '\\*', recursive=False)

    files = []
    for filePath in filesPaths:
        tuple = ("files", (filePath, open(filePath, 'rb'), 'application/octet-stream'))
        files.append(tuple)

    return files


def writeFile(request, workspaceName, datasetName, nodeId):
    fileName = workspaceName + "_" + datasetName + "_" + nodeId + "_" + str(datetime.now().microsecond) + ".laz"
    with open(fileName, "wb") as f:
        f.write(request.content)


def mergeFiles():
    inputFiles = "*.laz"
    output = "merge.laz"
    args = ["lasmerge", "-i", inputFiles, "-o", output]
    subprocess.call(args)
    return output


def visualizeFromLasTools():
    fileToVisualize = mergeFiles()
    args = ["lasview", fileToVisualize]
    subprocess.call(args)


def deleteLazFiles():
    files_in_directory = os.listdir(os.getcwd())
    filtered_files = [file for file in files_in_directory if file.endswith(".laz")]
    for file in filtered_files:
        os.remove(file)
