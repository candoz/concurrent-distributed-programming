#!/bin/bash
mkdir root

cd root

NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)

mkdir -p {a,b}/{e,f,g}/{c,d} && 
touch {a,b}/test{0001..0005}.txt && 
touch {a,b}/{e,f,g}/test{0001..0005}.txt && 
touch {a,b}/{e,f,g}/{c,d}/test{0001..0005}.txt

for p in {a,b}/{e,f,g}/{c,d}
	do 
	angle=$(($RANDOM%61-30))
	echo "Hello World $angle $NEW_UUID" > text.txt
	cp text.txt $p 
done



