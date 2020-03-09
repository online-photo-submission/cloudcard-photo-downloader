rm *.jpg
rm C*.udf
rm *.txt
echo .
echo Temp Directory
ls temp
rm -rf temp
echo .
echo Summary Directory
ls summary
echo .
echo Summary File Contents
cat summary/*.txt
rm -rf summary
