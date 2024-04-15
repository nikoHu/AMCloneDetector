import os
import time

measureIndexFile = './result/measure-index.csv'
resultGroupFile = './result/common-pair.csv'
measures = dict()
groups = []

def read_lines(path):
    lines = list()
    try:
        f = open(path, 'r', encoding='utf8')
        lines = f.readlines()
        f.close()
    except BaseException as e:
        pass
    return lines


def init():
    with open(measureIndexFile, 'r') as f:
        for line in f:
            infos = line.split(',')
            measure = dict()
            measure['id'] = infos[0]
            measure['path'] = ''.join(infos[1:-2])
            measure['start'] = int(infos[-2])
            measure['end'] = int(infos[-1])
            measures[infos[0]] = measure
    with open(resultGroupFile, 'r') as f:
        for line in f:
            infos = line.split(',')
            infos = [id.strip() for id in infos]
            groups.append(infos)


def printPairs():
    cnt = 1
    for group in groups:
        with open('pairs/%d.txt' % cnt, 'w', encoding='utf8') as f:
            cnt += 1
            if cnt % 100 == 0 or cnt == len(groups):
                print(cnt)
            for id in group:
                measure = measures[id]
                lines = read_lines(measure['path'])
                if len(lines) == 0:
                    continue
                lines = lines[measure['start'] - 1: measure['end']]
                content = ''.join(lines)
                f.write(id + '-----------------------------\n')
                f.write('%s ### %d - %d\n' % (measure['path'], measure['start'], measure['end']))
                f.write(content + '\n')

if __name__ == '__main__':
    print('init...')
    init()
    if not os.path.exists('pairs'):
        os.mkdir('pairs')
    print('extract...')
    printPairs()
    print('finish')



