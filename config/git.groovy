gradle.allprojects {

	ext.GIT = "git"

	ext.getGitBranch = {
		def String rBranch
		if (project.hasProperty("branch")) {
			def branches = project.getProperty("branch").split('/')
			def branch =branches[branches.length-1]
			logger.info("Using parameter branch $branch")
			rBranch = branch
		} else {
			def gitExe = GIT
			try {
				def stdout = new ByteArrayOutputStream()
				exec {
					executable = GIT
					args = [
						'rev-parse',
						'--abbrev-ref',
						'HEAD'
					]
					standardOutput = stdout
				}
				def currentBranch = stdout.toString().trim()
				if (currentBranch.contains("release/")) {
					rBranch = "candidate"
				} else {
					def lastPath = currentBranch.split('/')
					rBranch = lastPath[lastPath.length-1]
				}
			} catch(Exception e) {
				e.printStackTrace()
				return "local"
			}
		}
		if (rBranch.contains("release/")) {
			return "RC"
		}
		if (rBranch.contains("master")) {
			return ""
		}

		//println("Branch $rBranch")
		return rBranch
	}

	/**
	 * Version rule management.
	 */
	//logger.info "version $version"
	project.version = getGitBranch().isEmpty() ? version : (version + '.' + getGitBranch())
	ext.branch = getGitBranch()
}