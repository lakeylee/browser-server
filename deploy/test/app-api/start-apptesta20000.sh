nohup java -jar app-api.jar  --spring.profiles.active=apptesta20000 > /dev/null 2>&1 &
ps -elf|grep active=apptesta
