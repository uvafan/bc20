import math

round_flooded = [0] * 2501
elev = 1

for rnd in range(4001):
    elevation = math.exp(.0028*rnd - 1.38 * math.sin(.00157*rnd-1.73)+1.38*math.sin(-1.73)) - 1
    while elevation > elev:
        round_flooded[elev] = rnd
        elev += 1
        if elev >= len(round_flooded):
            break
    if elev >= len(round_flooded):
        break

for elev in range(len(round_flooded)):
    print('case {}: return {};'.format(elev,round_flooded[elev]))
