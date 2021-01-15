python app/webber.py > webber.debug 2>&1 &
echo $!
sleep 5
python app/whapper.py