rm *.jpg
rm C*.udf
rm *.txt
echo
echo Photo Directory
ls downloaded-photos
rm -rf downloaded-photos
echo
echo Summary Directory
ls summary
echo
echo Summary File Contents
cat summary/*.txt
rm -rf summary
