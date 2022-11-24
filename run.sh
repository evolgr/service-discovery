#!/bin/bash 

scriptName=`basename "$0"`
#echo ${scriptName}
scriptPath=`dirname "$0"`
#echo ${scriptPath}
currentPath=$PWD
export NS=$1
export TAG=$2

if [[ -z $NS ]]; then
	echo "Error: Namespace not specified";
	echo "please use: ${scriptPath}/${scriptName} <NAMESPACE>"
	exit 1
else
	result=$(kubectl get ns | grep $NS | wc -l 2>/dev/null)
	if [ $result -eq 0 ]; then
		echo "Error: Namespace does not exist or wrong namespace input used"
		echo "please use: ${scriptPath}/${scriptName} <NAMESPACE> <DOCKER-TAG>"
		echo "check for available namespaces: kubectl get namespace"
		exit 1
	elif [ $result -ne 1 ]; then
		echo "Error: More than one namespace much input"
		echo "please use: ${scriptPath}/${scriptName} <NAMESPACE> <DOCKER-TAG>"
		echo "check for available namespaces: kubectl get namespace"
		exit 1
	else
		echo "Namespace verified successfully"
	fi
fi

if [[ -z $TAG ]]; then
	echo "Error: Docker TAG not specified";
	echo "please use: ${scriptPath}/${scriptName} <NAMESPACE> <DOCKER-TAG>"
	echo "DOCKER-TAG is used:"
	echo "- to push images to dockerhub (try not to overwrite latest)"
	echo "- to load those docker images with specific tag in containers"
	exit 1
fi

cleanContainers(){
	result=$(docker container ls | grep best | wc -l)
	if [ $result -gt 1 ]; then
		docker rm -f $(docker container ls | grep best | awk '{ print $3 }')
	else
		echo "No best docker containers identified"
	fi
}

cleanDockerImages() {
	result=$(docker images -a | grep best | wc -l)
	if [ $result -gt 1 ]; then
		docker rmi -f $(docker images -a | grep best | awk '{ print $3 }')
	else
		echo "No best docker images identified"
	fi
}

deleteOldExecutableFiles() {
	if [ -f "${scriptPath}/code/chat-server/chat-server.jar" ]; then
		rm -rf ${scriptPath}/code/chat-server/chat-server.jar
		if [ -f "${scriptPath}/code/chat-server/chat-server.jar" ]; then
			echo "Failed to remove Server Executable from helm chart files";
		else
			echo "Server Executable successfully removed from helm chart files";
		fi
	else
		echo "Server Executable not found in server helm chart files";
	fi
	if [ -f "${scriptPath}/target/chat-server.jar" ]; then
		rm -rf ${scriptPath}/target/chat-server.jar
		if [ -f "${scriptPath}/target/chat-server.jar" ]; then
			echo "Failed to remove Server Executable from maven generated files";
		else
			echo "Server Executable successfully removed from maven generated files";
		fi
	else
		echo "Server Executable not found in maven generated files";
	fi
	if [ -f "${scriptPath}/charts/service-discovery/chat-server-1.0.tgz" ]; then
		rm -rf ${scriptPath}/charts/service-discovery/chat-server-1.0.tgz
		if [ -f "${scriptPath}/charts/service-discovery/chat-server-1.0.tgz" ]; then
			echo "Failed to remove Server helm chart from local files";
		else
			echo "Server helm chart successfully removed from local files";
		fi
	fi
	if [ -f "${scriptPath}/code/chat-server/sd-handler.jar" ]; then
		rm -rf ${scriptPath}/code/chat-server/sd-handler.jar
		if [ -f "${scriptPath}/code/chat-server/sd-handler.jar" ]; then
			echo "Failed to remove Server Executable from helm chart files";
		else
			echo "Server Executable successfully removed from helm chart files";
		fi
	else
		echo "Server Executable not found in server helm chart files";
	fi
	if [ -f "${scriptPath}/target/sd-handler.jar" ]; then
		rm -rf ${scriptPath}/target/sd-handler.jar
		if [ -f "${scriptPath}/target/sd-handler.jar" ]; then
			echo "Failed to remove Server Executable from maven generated files";
		else
			echo "Server Executable successfully removed from maven generated files";
		fi
	else
		echo "Server Executable not found in maven generated files";
	fi
	if [ -f "${scriptPath}/charts/service-discovery/sd-handler-1.0.tgz" ]; then
		rm -rf ${scriptPath}/charts/service-discovery/sd-handler-1.0.tgz
		if [ -f "${scriptPath}/charts/service-discovery/sd-handler-1.0.tgz" ]; then
			echo "Failed to remove Server helm chart from local files";
		else
			echo "Server helm chart successfully removed from local files";
		fi
	fi
	if [ -f "${scriptPath}/code/chat-server/sd-registry.jar" ]; then
		rm -rf ${scriptPath}/code/chat-server/sd-registry.jar
		if [ -f "${scriptPath}/code/chat-server/sd-registry.jar" ]; then
			echo "Failed to remove Server Executable from helm chart files";
		else
			echo "Server Executable successfully removed from helm chart files";
		fi
	else
		echo "Server Executable not found in server helm chart files";
	fi
	if [ -f "${scriptPath}/target/sd-registry.jar" ]; then
		rm -rf ${scriptPath}/target/sd-registry.jar
		if [ -f "${scriptPath}/target/sd-registry.jar" ]; then
			echo "Failed to remove Server Executable from maven generated files";
		else
			echo "Server Executable successfully removed from maven generated files";
		fi
	else
		echo "Server Executable not found in maven generated files";
	fi
	if [ -f "${scriptPath}/charts/service-discovery/sd-registry-1.0.tgz" ]; then
		rm -rf ${scriptPath}/charts/service-discovery/sd-registry-1.0.tgz
		if [ -f "${scriptPath}/charts/service-discovery/sd-registry-1.0.tgz" ]; then
			echo "Failed to remove Server helm chart from local files";
		else
			echo "Server helm chart successfully removed from local files";
		fi
	fi
}

buildCode(){
	cd ${scriptPath}
    mvn clean install
    \cp ${scriptPath}/target/chat-server.jar ${scriptPath}/code/chat-server/chat-server.jar
    \cp ${scriptPath}/target/sd-handler.jar ${scriptPath}/code/sd-handler/sd-handler.jar
    \cp ${scriptPath}/target/sd-registry.jar ${scriptPath}/code/sd-registry/sd-registry.jar
	cd ${currentPath}
}

buildImage() {
	echo Building image of $1
	cd ${scriptPath}/code/$1
	docker build ./ --file ./Dockerfile -t "evolgr/best-$1:$2";
	cd ${currentPath}
}

pushImage() {
	echo Pushing image of $1
	docker push "evolgr/best-$1:$2";
}

packageHelm() {
	echo Packaging of $1 chart
	helm package ${scriptPath}/charts/$1 -d ${scriptPath}/charts/service-discovery
}

undeploy() {
	result=$(helm list -n $NS | grep best-$1 | wc -l)
	if [ $result -eq 1 ]; then
		helm uninstall best-$1 -n $NS
	else
		echo "No best-$1 release identefied"
	fi
}

deploy() {
	helm install best-$1 ${scriptPath}/charts/service-discovery/${1}-1.0.tgz -n $NS --set image.${1}.tag=$2
}

runContainer() {
	echo Running $1
	docker run -d ${scriptPath}/best-$1;
}


main(){
	echo "======================================="
	echo "BEST DEMO automatic deploy initiated"
	echo "======================================="
	echo "Clearing Environment & Files"
	echo "======================================="
    # cleanContainers
    # cleanDockerImages
    # deleteOldExecutableFiles
	echo "======================================="
	echo "Undeploy"
	echo "======================================="
    # undeploy chat-server
    # undeploy sd-handler
    # undeploy sd-registry
	echo "======================================="
	echo "Building code"
	echo "======================================="
    buildCode
	echo "======================================="
	echo "Building images"
	echo "======================================="
    # buildImage chat-server $TAG
    # buildImage sd-handler $TAG
    # buildImage sd-registry $TAG
	echo "======================================="
	echo "Pushing the images to evolgr"
	echo "======================================="
    # pushImage chat-server $TAG
    # pushImage sd-handler $TAG
    # pushImage sd-registry $TAG
	echo "======================================="
	echo "Packaging helm"
	echo "======================================="
    # packageHelm chat-server
    # packageHelm sd-handler
    # packageHelm sd-registry
	echo "======================================="
	echo "Deploying"
	echo "======================================="
    # deploy chat-server $TAG
    # deploy sd-handler $TAG
    # deploy sd-registry $TAG
	echo "======================================="
    # runContainer server
    # runContainer client
    # echo =======================================
    # echo =======================================
    # runContainer server
    # runContainer client
    # echo =======================================
	echo "BEST Demo automatic Deploy complete"
	echo "======================================="
}

main
