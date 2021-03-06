package ozone.owf.grails.services

import org.springframework.context.*
import net.sf.ehcache.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONException
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import ozone.owf.grails.OwfException
import ozone.owf.grails.OwfExceptionTypes

class ThemeService {
	
    def preferenceService
    def mergedDirectoryResourceService

    def grailsApplication
    
    def getImageURL(def params) {
		def imageName = params.img_name

        def theme = getTheme(grailsApplication.mainContext.OzoneConfiguration.defaultTheme)
            
        //images may be found in two places.  They may be in the images folder under the folder for the curent theme.
        //If not, fall back to the image in the themes/common directory
        def imgUrl = "${theme.base_url}/images/${((params.isImageReqAdmin == true) ? 'admin/':'')}${imageName}"
        if (grailsApplication.mainContext.getResource(imgUrl).exists())
            return "/" + imgUrl

        return "/themes/common/images/${((params.isImageReqAdmin == true) ? 'admin/':'')}${imageName}"
    }

	
    def getAvailableThemes() {
        //find all files in the webapp (and external themes dir) 
        //with a path like 'themes/*.theme/theme.json'
        mergedDirectoryResourceService.getResources(
            grailsApplication.config.owf.external.themePath, 
            'themes', 
            "*.theme/theme.json"
        ).collect {
            try {
                return getThemeDefinitionFromResource(it)
            }
            catch (OwfException e) {
                return null
            }
        } - null //remove any nulls from the list
    }

    def getTheme(def themeName) {
        def resource = mergedDirectoryResourceService.getResource(
            grailsApplication.config.owf.external.themePath, 
            'themes', 
            "${themeName}.theme/theme.json")

        if (!(themeName && resource.exists()))
            throw new OwfException(message: "Cannot find the requested CSS theme: ${themeName}",
                exceptionType: OwfExceptionTypes.GeneralServerError)

        else
            return getThemeDefinitionFromResource(resource)
    }

    def getCurrentTheme() {
        //fall back to the default theme
        def defaultThemeName = grailsApplication.mainContext.OzoneConfiguration.defaultTheme
        def theme = getTheme(defaultThemeName)
        return theme
    }

    def getThemeDefinitionFromResource(def resource) {
        try {
            JSON.parse(new InputStreamReader(resource.inputStream))
        }
        catch (e) {
            log.error("Error while attempting to read Theme definition in ${resource.getURL()}. Exception: ${e}")

            throw new OwfException(message: 'Error retrieving the requested CSS theme',
                exceptionType: OwfExceptionTypes.GeneralServerError)
        }
    }
    

}

