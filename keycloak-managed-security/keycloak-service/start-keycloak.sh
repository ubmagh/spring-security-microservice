#!/usr/bin/env bash

PASSWORD="admin"
USERNAMEKEYCLOAK="admin"
REALM="my-ecom-realm"
CLIENT="products-app"
CONTAINER="mykeycloak"
user1="user1"
user1password="12345"
user2="user2"
user2password="12345"
user3="user3"
user3password="12345"
adminUser="admin"
adminPassword="12345"
declare -a ROLES=("USER" "ADMIN" "MANAGER" )


printf "\n👉 Pulling KeyCloak image .... \n"
docker pull quay.io/keycloak/keycloak:20.0


string=$( docker ps -a | grep "$CONTAINER" )
len=`expr length "$string"`

if [[ $len -gt 0 ]]
then
  printf "\n👉 deleting existing keyCloak container & volume\n"
  docker stop "$CONTAINER"
  docker rm "$CONTAINER"
fi

docker run -d --name $CONTAINER -p 8080:8080 -p 8443:8443 -e KEYCLOAK_HOME="/opt/keycloak" -e REALM="${REALM}" -e PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/opt/keycloak/bin" -e KEYCLOAK_ADMIN="${USERNAMEKEYCLOAK}" -e KEYCLOAK_ADMIN_PASSWORD="${PASSWORD}" quay.io/keycloak/keycloak:20.0 start-dev
until [ $(docker inspect -f {{.State.Running}} $CONTAINER ) == "true" ]; do
    sleep 1;
done;
sleep 50;

printf "\n✔ started successfuly Keycloak container \n"


printf "\n\t  - authenticating ...\n"
echo docker exec $CONTAINER kcadm.sh config credentials --server 'http://localhost:8080' --realm master --client admin-cli --user $USERNAMEKEYCLOAK --password $PASSWORD
docker exec $CONTAINER kcadm.sh config credentials --server 'http://localhost:8080' --realm master --client admin-cli --user $USERNAMEKEYCLOAK --password $PASSWORD


printf "\n👉 Deleting existing REALM:  ...\n"
docker exec $CONTAINER kcadm.sh delete realms/$REALM -r "$REALM"

printf "👉 adding a realm:  ...\n"

printf "\n\t  - Creating ${REALM} realm ...\n"
docker exec "$CONTAINER" kcadm.sh create realms -s realm="${REALM}" -s enabled=true

printf "\n\t  - authenticating to the realm ${REALM}  ...\n"
docker exec $CONTAINER kcadm.sh config credentials --server 'http://localhost:8080' --realm $REALM --client admin-cli --user $USERNAMEKEYCLOAK --password $PASSWORD


printf "\n\t  - Creating client : ${CLIENT} ...\n"
#echo docker exec "$CONTAINER" kcadm.sh create clients -r $REALM -s clientId=$CLIENT -s protocol="openid-connect" -s name="$CLIENT-client"  -s "redirectUris=[\"http://localhost:8081/*\"]"
docker exec "$CONTAINER" kcadm.sh create clients -r $REALM -s clientId=$CLIENT -s protocol="openid-connect" -s name="$CLIENT-client" -s "redirectUris=[\"http://localhost:8081/*\"]"


LINE=$(docker exec "$CONTAINER" kcadm.sh get clients -r $REALM --fields 'id,clientId' | grep $CLIENT -B 1)
array=($(echo $LINE | sed 's/,/ /g'))
element=${array[3]}
clientid=$(echo $element | tr -d '"')

printf "\n\t  - UPDATING client : ${CLIENT} with root url ...\n"
docker exec "$CONTAINER" kcadm.sh update clients/$new_string -r $REALM -s 'redirectUris=["http://localhost:8081/*"]' 'rootUrl=["http://localhost:8081/*"]'


printf "\n👉 Creating users "

printf "\n\t  - Creating user  : ${user1} ..."
userid=$(docker exec $CONTAINER  kcadm.sh create users -r $REALM -s username="${user1}" -s firstName=Hassan -s lastName=Hassani -s emailVerified=true -s enabled=true -o --fields id | sed -n 2p)
array=($(echo $userid | sed 's/:/ /g'))
element=${array[1]}
user1id=$(echo $element | tr -d '"')
#printf "=> userid : "$userid

printf "\n\t  - update user  : ${user1} password ..."
docker exec $CONTAINER kcadm.sh update users/$user1id/reset-password -r $REALM -s type=password -s value="$user1password" -s temporary=false -n



printf "\n\t  - Creating user  : ${user2} ..."
userid=$(docker exec $CONTAINER  kcadm.sh create users -r $REALM -s username="${user2}" -s firstName=Mohamed -s lastName=Ali -s emailVerified=true -s enabled=true -o --fields id | sed -n 2p)
array=($(echo $userid | sed 's/:/ /g'))
element=${array[1]}
user2id=$(echo $element | tr -d '"')

printf "\n\t  - update user  : ${user2} password ..."
docker exec $CONTAINER kcadm.sh update users/$user2id/reset-password -r $REALM -s type=password -s value="$user2password" -s temporary=false -n




printf "\n\t  - Creating user  : ${user3} ..."
userid=$(docker exec $CONTAINER  kcadm.sh create users -r $REALM -s username="${user3}" -s firstName=ILHAM -s lastName=Ali -s emailVerified=true -s enabled=true -o --fields id | sed -n 2p)
array=($(echo $userid | sed 's/:/ /g'))
element=${array[1]}
user3id=$(echo $element | tr -d '"')

printf "\n\t  - update user  : ${user3} password ..."
docker exec $CONTAINER kcadm.sh update users/$user3id/reset-password -r $REALM -s type=password -s value="$user3password" -s temporary=false -n




printf "\n\t  - Creating admin user  : ${adminUser} ..."
userid=$(docker exec $CONTAINER  kcadm.sh create users -r $REALM -s username="${adminUser}" -s firstName=Ayoub -s lastName=youba -s emailVerified=true -s enabled=true -o --fields id | sed -n 2p)
array=($(echo $userid | sed 's/:/ /g'))
element=${array[1]}
adminid=$(echo $element | tr -d '"')

printf "\n\t  - update user  : ${adminUser} password ..."
docker exec $CONTAINER kcadm.sh update users/$adminid/reset-password -r $REALM -s type=password -s value="$adminPassword" -s temporary=false -n



printf "\n\n👉 Creating Roles "
for role in ${ROLES[@]}; do
  docker exec $CONTAINER kcadm.sh create roles -r $REALM -s name="$role"
done


printf "\n\n👉 Attributing roles to users  "

docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user1 --rolename USER
printf "\n\t  - added 'USER' to user1 "

docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user2 --rolename USER
docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user2 --rolename ADMIN
printf "\n\t  - added 'USER' and 'ADMIN' to user2 "

docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user3 --rolename USER
docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user3 --rolename ADMIN
docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $user3 --rolename MANAGER
printf "\n\t  - added 'USER', 'MANAGER' and 'ADMIN' to user3 "


docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $adminUser --rolename USER
docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $adminUser --rolename ADMIN
docker exec $CONTAINER kcadm.sh add-roles -r $REALM --uusername $adminUser --rolename MANAGER
printf "\n\t  - added 'USER', 'MANAGER' and 'ADMIN' to admin "








