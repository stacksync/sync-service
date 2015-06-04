__author__ = 'sergi'

import math

def analyse_file(file_name, threshold):
    times = []
    with open(file_name, 'r') as f:
        next(f)
        for line in f:
            time, operation, node_id, node_type, mime, size, user = line.split(",")

            times.append(time)

    # get diffs
    diff = []
    last = float(times.pop(0))

    while len(times) > 0:
        t = float(times.pop(0))
        diff.append(t - last)
        last = t

    # get mean
    mean = 0
    for x in diff:
        mean += x

    mean /= len(diff)

    # get deviation
    sum = 0
    for x in diff:
        sum += (x-mean)*(x-mean)

    dev = math.sqrt(sum/len(diff))

    print "-----------------"
    print file_name
    print "Mean = ", mean
    print "Dev = ", dev

    count = 0
    for x in diff:
        if x <= threshold:
            count += 1

    print "Count = ", count, " /", len(diff)

files = ['hour_parsed.csv']
for i in xrange(0,10):
    files.append('node'+str(i)+'.csv')

for f in files:
    analyse_file(f, 0.01)