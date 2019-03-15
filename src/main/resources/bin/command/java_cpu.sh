#!/bin/bash
# ----------------------------------------------------------------------------
# Jpom 导出
# ----------------------------------------------------------------------------

T=`ps -mp $1 -o THREAD,tid,time|sort -k 2 -nr|awk '{print $2","$8","$9}'|head -n 11|grep -v "-"`

Count=$2
Name=$3
if [[ "$Name" = ""  ]]; then
   Name="java_cpu.txt";
fi

touch ${Name}

: > ${Name}

cur_dateTime="`date +%Y-%m-%d_%H:%M:%S`"

echo "生成时间：$cur_dateTime" >>${Name}

echo "进程id:$T"

for i in ${T}
do
consum=`echo ${i} |awk -F"," '{print $1}'`
tid=`echo ${i} |awk -F"," '{print $2}'`
dtime=`echo ${i} |awk -F"," '{print $3}'`

echo  "线程id：$tid"

id=`printf "%x\n" ${tid}`
echo "线程消耗CPU：$consum% 运行时间：$dtime 线程id：$id" >> ${Name}
jstack $1 |grep ${id} -A ${Count} >> ${Name}
echo "">> ${Name}
echo "===========================================================================================" >> ${Name}

done
