# AMCloneDetector
方法级/文件级克隆代码检测工具

配置如下：

行差比（两个方法行差的绝对值 / 短方法行数）-矩阵比较的时候，行差比大于0.5的不比较，起到加速作用

line-gap-dis=0.5

矩阵中1的个数(两个方法中相似行的个数) / 短方法行数 - 相似度大于该值的才判定为克隆

similarity=0.8

存有方法信息的文件路径

measure-index-path=C:\\Users\\10146\\Downloads\\test\\test\\detect\\result\\MeasureIndex.csv

1-字符串比较；2-simhash

compare-type=1

方法最小行数

min-line=10
