#!/bin/bash -x

scriptName=`basename "$0"`
#echo ${scriptName}
scriptPath=`dirname "$0"`
#echo ${scriptPath}
currentPath=$PWD
echo "Automated deployment script ${scriptPath}/${scriptName} started from location ${currentPath}"

export NS=$1
export VERSION=$2

if [[ -z $NS ]]; then
	echo "Error: Namespace not specified";
	echo "please use: ${scriptPath}/${scriptName} <NAMESPACE>"
	exit 1
else
	result=$(kubectl get ns | grep $NS | wc -l 2>/dev/null)
	if [ $result -eq 0 ]; then
		echo "Error: Namespace does not exist or wrong namespace input used"
		echo "Pease select valid namespace and use command:"
		echo "${scriptPath}/${scriptName} <NAMESPACE> <VERSION>"
		exit 1
	elif [ $result -ne 1 ]; then
		echo "Error: More than one namespace much input"
		echo "please use: ${scriptPath}/${scriptName} <NAMESPACE> <VERSION>"
		echo "check for available namespaces: kubectl get namespace"
		exit 1
	else
		echo "Namespace verified successfully"
	fi
fi

if [[ -z $VERSION ]]; then
	echo "Error: VERSION not specified";
	echo "please use: ${scriptPath}/${scriptName} <NAMESPACE> <VERSION>"
	echo "VERSION is used:"
	echo "- for uploading images to dockerhub"
	echo "- for setting those images in respective charts for used containers"
	echo "- for setting chart versions"
	exit 1
else
	echo "VERSION ${VERSION} will be used"
fi

cleanDockerImages() {
	result=$(docker images -a | grep 'chat-\|sd' | wc -l)
	if [ $result -gt 1 ]; then
		docker rmi -f $(docker images -a | grep 'chat-\|sd' | awk '{ print $3 }')
	else
		echo "No best docker images identified"
	fi
}

deleteGeneratedFiles() {
	result2=0
	mvn clean
	result1=$?
	if [ -d "${scriptPath}/.output" ]; then
		rm -R ${scriptPath}/.output
		result2=$?
	fi
	if [ ${result1} -ne 0 ] || [ ${result2} -ne 0 ]; then
		echo "Clean failed."
		exit 1
	fi
}

buildCode(){
	cd ${scriptPath}
    mvn clean install
	cd ${currentPath}
}

# input:
# 1 -> image name
# 2 -> image version/tag
buildImage() {
	echo "Building image of $1 with version $2"
	cd ${scriptPath}/code/$1
	docker build ./ --file ./Dockerfile -t "evolgr/$1:$2"
	result=$?
	cd ${currentPath}
	if [ ${result} -ne 0 ]; then
		echo "Docker image creation failed"
		exit 1
	fi
}

# input:
# 1 -> image name
# 2 -> image version/tag
pushImage() {
	echo "Pushing image of $1 with version $2"
	docker push "evolgr/$1:$2";
}

packageHelm() {
	echo "Packaging of $1 chart"
	if [ ! -d "${scriptPath}/.output" ] 
	then
		echo "Creating directory ${scriptPath}/.output" 
		mkdir -p ${scriptPath}/.output/$1
	fi
	cp -fR ${scriptPath}/charts/$1 ${scriptPath}/.output/
	
	sed -i "s/0.0.1/${VERSION}/g" ${scriptPath}/.output/$1/Chart.yaml
	sed -i "s/0.0.1/${VERSION}/g" ${scriptPath}/.output/$1/values.yaml
	helm package ${scriptPath}/.output/$1 -d ${scriptPath}/.output
}

packageIntegrationHelm() {
	echo "Packaging of integration chart"
	if [ ! -d "${scriptPath}/.output" ] 
	then
		echo "Mandatory output directory does not exist" 
		exit 1
	fi
	if [ ! -d "${scriptPath}/.output/sd-handler" ] 
	then
		echo "Mandatory sub-chart sd-handler does not exist" 
		exit 1
	fi
	if [ ! -d "${scriptPath}/.output/sd-registry" ] 
	then
		echo "Mandatory sub-chart sd-registry does not exist" 
		exit 1
	fi
	if [ ! -d "${scriptPath}/.output/chat-server" ] 
	then
		echo "Mandatory sub-chart chat-server does not exist" 
		exit 1
	fi
	
	## copy integration chart base files
	cp -fR ${scriptPath}/charts/service-discovery ${scriptPath}/.output/
	
	# update integration chart version
	sed -i "s/0.0.1/${VERSION}/g" ${scriptPath}/.output/service-discovery/Chart.yaml
	
	# copy sub-charts to charts folder
	cp -fR ${scriptPath}/.output/sd-handler ${scriptPath}/.output/service-discovery/charts
	cp -fR ${scriptPath}/.output/sd-registry ${scriptPath}/.output/service-discovery/charts
	cp -fR ${scriptPath}/.output/chat-server ${scriptPath}/.output/service-discovery/charts
	
	# update sub-chart versions
	sed -i "s/0.0.1/${VERSION}/g" ${scriptPath}/.output/service-discovery/requirements.yaml
	helm package ${scriptPath}/.output/service-discovery -d ${scriptPath}/.output
}

# input:
# 1 -> chart name
undeploy() {
	result=$(helm list -n $NS | grep $1 | wc -l)
	if [ $result -eq 1 ]; then
		helm uninstall $1-${USER} -n $NS
	else
		echo "There are no helm charts with $1 release identefied"
	fi
}

deploy() {
	helm install ${1}-${USER} ${scriptPath}/.output/${1}-${2}.tgz -n $NS
}

main(){
	echo "======================================="
	echo "BEST DEMO automatic deploy initiated"
	echo "======================================="
	echo "Clearing Environment & Files"
	echo "======================================="
    cleanDockerImages
    deleteGeneratedFiles
	echo "======================================="
	echo "Undeploy"
	echo "======================================="
    # undeploy chat-server
    # undeploy sd-handler
    # undeploy sd-registry
    undeploy service-discovery
	echo "======================================="
	echo "Building code"
	echo "======================================="
    buildCode
	echo "======================================="
	echo "Building images"
	echo "======================================="
    buildImage chat-server $VERSION
    buildImage sd-handler $VERSION
    buildImage sd-registry $VERSION
	echo "======================================="
	echo "Pushing the images to evolgr registry"
	echo "======================================="
    pushImage chat-server $VERSION
    pushImage sd-handler $VERSION
    pushImage sd-registry $VERSION
	echo "======================================="
	echo "Packaging helm"
	echo "======================================="
    packageHelm chat-server
    packageHelm sd-handler
    packageHelm sd-registry
	echo "======================================="
	echo "Packaging integration helm chart"
	echo "======================================="
	packageIntegrationHelm
	echo "======================================="
	echo "Deploying"
	echo "======================================="
    # deploy chat-server $VERSION
    # deploy sd-handler $VERSION
    # deploy sd-registry $VERSION
    deploy service-discovery $VERSION
    echo =======================================
	echo "BEST Demo automatic Deploy complete"
	echo "======================================="
}

main
