#!/usr/bin/env nu

# ast-grep scan --rule cli/extractors/get-commands.yml --json | jq -r '.[0].text'
# ast-grep scan --rule cli/extractors/get-events.yml --json | jq -r '.[].text'
# ast-grep scan --rule cli/extractors/get-events-names.yml --json diagrams/events.scala | jq -r '.[].text'
#ast-grep scan --rule cli/extractors/get-parameters-names.yml --json diagrams/class.scala | jq -r '.[].text'

ast-grep scan --rule cli/extractors/get-parameters-names.yml --json diagrams/class.scala | jq -r '.[].text' | lines

