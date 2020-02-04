'''
## Alex - Hephaestus, a system for mass testing and tuning the high ground!
In order to use Alephaestus you need python. To run Hephaestus simply navigate to the battlecodeScaffold2020 root directory (be able to see the python file)
and run the following command.
python Alephaestus.py <pkg of team a> <pkg of team b>
flags:
	-x run both ways
	-m allows you to choose the maps by putting them in a space-separated string. does all the maps if this flag is missing
ex: python Hephaestus.py team008.jonahBot team008.defendbot00 -m "Lanes Alone" -x
Hephaestus will then play team a and team b against eachother on all the selected maps (in both positions if you tell it to). It should return a result like the following.
	running on maps:  ['Arena']
	Team A won on Arena
	flipping sides... (but not names: team008.jonahBot is still called team A but plays as blue)
	Team A won on Arena
	Team A won 2 games -- Team B won 0
When the starting positions are flipped the output will keep the original names.
Enjoy!
'''
import subprocess
import sys

def getMapNames():
	infile = open('mapsShort.txt', 'r') # r for read
	names = [line.strip() for line in infile]
	infile.close()
	return names

def runMatches(teamA, teamB, teamC, teamD, stats, maps, flipTeams=False):
	for mapName in maps:
		command = ['gradlew.bat', 'run', '-PteamA='+teamA, '-PteamB='+teamB, '-Pmaps='+mapName]
		print(command)
		result = subprocess.check_output(command, shell=True).decode("utf-8")
		winningTeam = result[result.find(') wins')-1]
		if str(winningTeam) == "B":
			winningTeam = 'B'
		else: 
			winningTeam = 'AB'
		stats[winningTeam]+=1
		print ('Team ' + winningTeam + ' won on '+mapName)
		print()

		command = ['gradlew.bat', 'run', '-PteamA='+teamA, '-PteamB='+teamC, '-Pmaps='+mapName]
		print(command)
		result = subprocess.check_output(command, shell=True).decode("utf-8")
		winningTeam = result[result.find(') wins')-1]
		if str(winningTeam) == "B":
			winningTeam = 'C'
		else: 
			winningTeam = 'AC'
		stats[winningTeam]+=1
		print ('Team ' + winningTeam + ' won on '+mapName)
		print()

		command = ['gradlew.bat', 'run', '-PteamA='+teamA, '-PteamB='+teamD, '-Pmaps='+mapName]
		print(command)
		result = subprocess.check_output(command, shell=True).decode("utf-8")
		winningTeam = result[result.find(') wins')-1]
		if str(winningTeam) == "B":
			winningTeam = 'D'
		else: 
			winningTeam = 'AD'
		stats[winningTeam]+=1
		print ('Team ' + winningTeam + ' won on '+mapName)
		print()

if __name__ == '__main__':

	mapNames = ''
	teamA = ''
	teamB = ''
	teamC = ''
	teamD = ''
	teamE = ''
	doBothDirs = True
	skipNext = False
	for i in range(1, len(sys.argv)):
		if teamC:
			teamD = sys.argv[i]
			print("Team D is: "+teamD)

		elif teamB and not teamC:
			teamC = sys.argv[i]
			print("Team C is: "+teamC)

		elif teamA and not teamB:
			teamB = sys.argv[i]
			print("Team B is: " +teamB)

		else:
			teamA = sys.argv[i]
			print("Team A is: " +teamA)


	if not mapNames:
		mapNames = getMapNames()
		gameCount = len(mapNames)*3
	print("running on maps: ")
	gameStats = {'AB': 0,'AC': 0,'AD': 0,'B': 0, 'C': 0, 'D': 0} #index zero for team A index one for Team B, etc.

	#Play all the maps
	runMatches(teamA, teamB, teamC, teamD, gameStats, mapNames)

	#Print the final results
	print('Team A beat team B ' + str(gameStats['AB']) +' games -- Team B won ' + str(gameStats['B'])+' games ')
	print('Team A beat team C ' + str(gameStats['AC']) +' games -- Team C won ' + str(gameStats['C'])+' games ')
	print('Team A beat team D ' + str(gameStats['AD']) +' games -- Team D won ' + str(gameStats['D'])+' games ')
	print('Team A won '+str(gameStats['AB']+gameStats['AC']+gameStats['AD'])+' games overall out of '+str(gameCount)+' games played')
