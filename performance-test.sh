#!/bin/bash

# Basic/regular test:-

# aws ec2 request-spot-instances --spot-price 0.07 --instance-count 2 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"r3.xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"

# Multiple instances with ~30GB RAM
#aws ec2 request-spot-instances --spot-price 0.05 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"r3.xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.05 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-6a3f7e1d\",\"InstanceType\":\"m2.2xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.17 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"m3.2xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.20 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"c3.4xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"

# Multiple instances with ~15GB RAM
#aws ec2 request-spot-instances --spot-price 0.05 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"r3.large\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.05 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-6a3f7e1d\",\"InstanceType\":\"m2.xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.10 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"m3.xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"
#aws ec2 request-spot-instances --spot-price 0.10 --instance-count 1 --launch-specification "{\"ImageId\":\"ami-7842030f\",\"InstanceType\":\"c3.2xlarge\",\"KeyName\":\"mtandy home i7 pc\",\"IamInstanceProfile\":{\"Name\":\"s3access\"},\"SecurityGroupIds\":[\"sg-2e039e59\"],\"BlockDeviceMappings\":[{\"VirtualName\":\"ephemeral0\",\"DeviceName\":\"/dev/sdb\"}],\"UserData\":\"`base64 performance-test.sh | tr -d '\n'`\"}"


#14.04 LTS & instance store & PV: ami-6a3f7e1d
#14.04 LTS & instance store & HVM: ami-7842030f <-- Supports r3.large (15gb, 4 cent spot), r3.xlarge (30gb, 7 cent spot)

#sudo shutdown -h 50 &

sudo chown ubuntu:ubuntu /mnt/
echo 'started' > /mnt/runtimes.txt
date >> /mnt/runtimes.txt

sudo apt-get update
sudo apt-get install -y openjdk-7-jdk awscli maven git cpuid
echo 'apt-get complete' >> /mnt/runtimes.txt
date >> /mnt/runtimes.txt

mkdir /mnt/ch
cd /mnt/ch

aws --region=us-west-1 s3 cp s3://ch-test-mjt/gb-binary-format-v5.tar.gz .
tar -xvf gb-binary-format-v5.tar.gz
md5sum great-britain-*

git clone https://github.com/michaeltandy/contraction-hierarchies.git

cd contraction-hierarchies
#git reset --hard aa0773713aec296f460dda1610cf7a1cb8689b09 # To test a particular git revision

git log -1 >> /mnt/runtimes.txt

echo 'got map and source code' >> /mnt/runtimes.txt
date >> /mnt/runtimes.txt

lsb_release -a
mvn --version
mvn help:system
mvn dependency:resolve dependency:resolve-plugins

echo 'Resolved maven dependencies' >> /mnt/runtimes.txt
date >> /mnt/runtimes.txt

mvn clean install
ch_git_rev=`git rev-parse master`

echo 'Compiled code' >> /mnt/runtimes.txt
date >> /mnt/runtimes.txt

cd /mnt/ch

#java -cp ch-1.0-SNAPSHOT.jar -Xmx24g -Xms8g uk.me.mjt.ch.LoadAndPathUk | tee LoadAndPathUk1.txt
instance_type=`curl http://169.254.169.254/latest/meta-data/instance-type`
instance_id=`curl http://169.254.169.254/latest/meta-data/instance-id`

if [ $instance_type = "r3.large" ] || [ $instance_type = "m2.xlarge" ] || [ $instance_type = "m3.xlarge" ] || [ $instance_type = "c3.2xlarge" ]
then
  java_memory="-Xmx14g -Xms14g";
else
  java_memory="-Xmx28g -Xms28g";
fi

for i in `seq 1 3`;
do
    java -cp "/mnt/ch/contraction-hierarchies/target/classes/:/mnt/ch/contraction-hierarchies/target/ch-1.0-SNAPSHOT.jar" $java_memory -XX:GCTimeLimit=60 uk.me.mjt.ch.BenchmarkUk | tee LoadAndPathUk-$i.txt
    aws --region=us-west-1 s3 cp LoadAndPathUk-$i.txt s3://ch-test-mjt/$ch_git_rev/$instance_type/$instance_id/
    uncached_pathing_time=`cat LoadAndPathUk-$i.txt | grep 'repetitions uncached pathing' | sed 's/.*in //' | sed 's/ ms.//'`
    cached_pathing_time=`cat LoadAndPathUk-$i.txt | grep 'repetitions cached pathing' | sed 's/.*in //' | sed 's/ ms.//'`
    parallel_pathing_time=`cat LoadAndPathUk-$i.txt | grep 'repetitions parallel uncached pathing' | sed 's/.*in //' | sed 's/ ms.//'`
    data_load_time=`cat LoadAndPathUk-$i.txt | grep 'Data load complete in ' | sed 's/.*in //' | sed 's/ms.//'`
    parallel_cached_time=`cat LoadAndPathUk-$i.txt | grep 'repetitions parallel cached pathing' | sed 's/.*in //' | sed 's/ ms.//'`
    curl "https://docs.google.com/forms/d/1xFOZk3D1wnIjB0N3bhNIirppMSug1qJChYbyA4JGAf0/formResponse?ifq&entry.1150050082=$instance_id&entry.1477423851=$instance_type&entry.592766186=$ch_git_rev&entry.915449080=$uncached_pathing_time&entry.884534640=$cached_pathing_time&entry_1158999837=$parallel_pathing_time&entry_1024384356=$data_load_time&entry_1412979057=$parallel_cached_time&submit=Submit" > curl-output.txt
done

echo 'test complete, shutting down' >> /mnt/runtimes.txt
date >> /mnt/runtimes.txt

#sudo apt-get install -y openjdk-7-jdk make nasm gcc awscli
#cd /mnt
#aws --region=us-west-1 s3 cp s3://ch-test-mjt/bandwidth-1.1b.tar.gz .
#tar -xvf bandwidth-1.1b.tar.gz
#sudo apt-get install -y make nasm gcc
#cd bandwidth-1.1
#make bandwidth64
#nice -n -2 ./bandwidth64 | tee bandwidth64-output.txt
#aws --region=us-west-1 s3 cp bandwidth64-output.txt s3://ch-test-mjt/$instance_type/$instance_id/
#aws --region=us-west-1 s3 cp bandwidth.bmp s3://ch-test-mjt/$instance_type/$instance_id/
#date >> /mnt/runtimes.txt

cpuid -1 > /mnt/cpuid.txt
aws --region=us-west-1 s3 cp /mnt/cpuid.txt s3://ch-test-mjt/$ch_git_rev/$instance_type/$instance_id/

aws --region=us-west-1 s3 cp /mnt/runtimes.txt s3://ch-test-mjt/$ch_git_rev/$instance_type/$instance_id/
aws --region=us-west-1 s3 cp /var/log/cloud-init-output.log s3://ch-test-mjt/$ch_git_rev/$instance_type/$instance_id/
aws --region=us-west-1 s3 cp /mnt/ch/hs_err_pid* s3://ch-test-mjt/$ch_git_rev/$instance_type/$instance_id/

sudo shutdown -h now

