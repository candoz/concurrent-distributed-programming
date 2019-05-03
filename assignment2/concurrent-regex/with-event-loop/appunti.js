'use strict'
const request = require('request')
const cheerio = require('cheerio');
const utils = require('./utils.js');

const MAX_REQUESTS = 10;
// const MAX_DEPTH = 2;
const ROOT_URL = process.argv[2];             // 0=cmd 1=entry-point 2=url
const RE = new RegExp("process.argv[3]".match(/[A-Za-z0-9$-/:-?{-~!"^_`\[\]]\s*/), 'g');  // 'g' for "global search"

let totalRequests = 0;
let currentDepth = 0;
let linksAlreadyFound = new Array();
let nLinksAlreadyProcessed = 0;
let nLinksWithMatches = 0;
let averageMatches = 0;

checkArgs();
linksAlreadyFound.push(ROOT_URL);
webCrawling(ROOT_URL);
totalRequests++;

function checkArgs() {
    if (process.argv.length < 4){
        process.stdout.write("USE: index.js <URL> <REGEXP>\n"); 
        process.exit(0);
    }
}

function webCrawling(url){
    return new Promise((resolve,reject)=>{
        request(url, function processLink(error, response, body) {
            if (response && response.statusCode == 200) {
                // if (currentDepth < MAX_DEPTH) {
                    resolve(response);
                    console.log("resolve(response)\n");

                    const $ = cheerio.load(body);
                    const linksInThisPage = utils.filterLinks($('a').map((i,x) => $(x).attr('href')).get());
                    const matches = utils.countRegexpMatches($.html(), RE);
                    
                    if (matches > 0) {
                        nLinksWithMatches++;
                        averageMatches = utils.updateAverage(averageMatches, nLinksWithMatches, matches);
                    }
                    nLinksAlreadyProcessed++;
        
                    let report = `Processed links: ${nLinksAlreadyProcessed} | ` +
                                   `With matches: ${(nLinksWithMatches*100/nLinksAlreadyProcessed).toFixed(2)}% | `;
                    if (averageMatches != 0) {
                        report += `Average matches when matching: ${averageMatches.toFixed(2)}`;
                    }
                    
                    if (matches > 0) {
                        utils.writeOverLastLine(`[${matches}] ${response.request.uri.href}\n` + report);
                    } else {
                        utils.writeOverLastLine(report);
                    }
                    
                    linksInThisPage.forEach(link => {
                        if (totalRequests < MAX_REQUESTS) {
                            if (!(link in linksAlreadyFound)) {
                                webCrawling(link, processLink);
                                totalRequests++;
                            }
                        }
                    });
                // }
            }else{
                console.log( "reject(error) \n");
                reject(error+"\n");
                
            }
        });
    })
}


