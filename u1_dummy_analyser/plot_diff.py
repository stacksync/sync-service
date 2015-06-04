__author__ = 'sergi'

def analyse_file(file_name):
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

    with open(file_name+'.dat', 'w') as f:
        for x in diff:
            f.write(str(x)+'\n')


files = []
for i in xrange(0,10):
    # files.append('node'+str(i)+'.csv')
    files.append('node_fifo_'+str(i)+'.csv')

for f in files:
    analyse_file(f)