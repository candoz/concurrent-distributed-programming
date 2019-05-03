"use strict";

const request = require('request'),
      cheerio = require('cheerio');

module.exports = {

    /**
     * Checks if the exact number of parameters is given, then returns the arguments in a proper Object
     * @returns {Obj} with fields rootUrl, regExp, maxRequests
     */
    getArgs() {
        if (process.argv.length != 5) {
            process.stdout.write("USE: node <ENTRY_POINT> <ROOT_URL> <REG_EXP> <MAX_REQUESTS>\n");
            process.exit(0);
        }
        let rootUrl = process.argv[2];
        return {  // 0=cmd, 1=entry-point
            'rootUrl': rootUrl,
            'regExp': new RegExp(process.argv[3], 'g'),  // 'g' for "global search"
            'maxRequests': process.argv[4]
        }
    },

    /**
     * Return a new array with only valid, absolute links.
     * This function strips out invalid links and convert all relative links to absolute links.
     * @param {Array} $         the HTML body of a webpage loaded into cheerio, similar to the $ alias for a jquery object
     * @param {String} protocol the protocol of the page where the raw links were found
     * @param {String} urlHost  the url of the page where the raw links were found
     * @returns {Array}         array of clean, absolute links
     */
    getLinks(response) {
        const $ = cheerio.load(response.body),
              protocol = response.request.uri.protocol,
              urlHost = response.request.uri.host;

        const rawLinks = $('a').map((i, x) => $(x).attr('href')).get();

        const otherExtensionsRE = new RegExp(/\.(jpeg|jpg|gif|png|pdf|mp4)$/),  // preemptive filter
              httpRE = new RegExp(/^http/),
              noProtocolRE = new RegExp(/^\/\//),
              relativeAddressRE = new RegExp(/^\//);
        
        let validLinks = new Array();

        for (let i=0; i<rawLinks.length; i++) {
            if (!((rawLinks[i] || '').match(otherExtensionsRE))) {  // exclude known not html extensions
                if ((rawLinks[i] || '').match(httpRE)) {
                    validLinks.push(rawLinks[i]);
                } else if((rawLinks[i] || '').match(noProtocolRE)) {
                    validLinks.push(protocol+rawLinks[i]);
                } else if ((rawLinks[i] || '').match(relativeAddressRE)){
                    validLinks.push(protocol+"//"+urlHost+rawLinks[i]);
                }
            }
        }
        return validLinks;
    },

    /**
     * Counts the matches of the RegExp in a response body
     * @param {String} response 
     * @param {RegExp} regExp 
     * @returns {number} of regExp matches in the body
     */
    countRegexpMatches(response, regExp) {
        const $ = cheerio.load(response.body);
        return (($('body').text() || '').match(regExp) || []).length;
    },

    /**
     * Update an average given the old average, the number of old values and the new value 
     * @param {number} oldAverage 
     * @param {number} howManyOldValues 
     * @param {number} newValue 
     */
    updateAverage(oldAverage, howManyOldValues, newValue) {
        return (oldAverage * howManyOldValues + newValue) / (howManyOldValues + 1);
    },

    /**
     * Prints the text overwriting the last line in the stdout
     * @param {String} text 
     */
    writeOverLastLine(text) {
        process.stdout.clearLine();
        process.stdout.cursorTo(0);
        process.stdout.write(text);
    },

    /**
     * Prints the stats overwriting the last line in the stdout
     * @param {number} nLinksAlreadyProcessed 
     * @param {number} nLinksAborted 
     * @param {number} nLinksWithMatches 
     * @param {number} averageMatches (while matching) 
     */
    writeStats(nLinksAlreadyProcessed, nLinksAborted, nLinksWithMatches, averageMatches){
        let stats = `Pages crawled: ${nLinksAlreadyProcessed+nLinksAborted} (aborted: ${nLinksAborted}) | ` +
                    `With matches: ${(nLinksWithMatches / nLinksAlreadyProcessed * 100).toFixed(1)}% `;
        if (averageMatches != 0) {
            stats += `(avg matches while matching: ${averageMatches.toFixed(2)}) `;
        }
        this.writeOverLastLine(stats);
    }
}
