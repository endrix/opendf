// variableChecks.nl

network variableChecks( goodParam, paramShadowedByVar ) inPort ==> outPort :

var

  varGood;
/*
  varShadowedByProcArg;
  varShadowedByFuncArg;
  varShadowedByGenerator;
  varShadowedByLetVar;

  varDuplicated;
  varDuplicated;
  
  paramShadowedByVar;

  varDuplicatedByFunction;
  varDuplicatedByProcedure;

  function functionGood( functionArgGood,
                         functionArgDuplicated,
                         functionArgDuplicated,
                         varShadowedByFuncArg ) :
    0
  end
  
  function varDuplicatedByFunction( functionArgGood ) :
    0
  end

  function functionDuplicated( functionArgGood ) :
    0
  end
  
  function functionDuplicated( functionArgGood ) :
    0
  end
  
  procedure varDuplicatedByProcedure( functionArgGood )
  begin
    varGood := 0;
  end

  procedure procedureDuplicated( functionArgGood )
  begin
    varGood := 0;
  end
 
  procedure procedureDuplicated( functionArgGood )
  begin
   varGood := 0;
  end
*/
entities
   
  goodEntity = goodEntity();

  list = [ someActor() /* let goodLetVar = 0, varShadowedByLetVar = 0 : someActor() end  */ :
             for generatorGood in Integers(1,10),
             for varShadowedByGenerator in Integers(1,10),
             for generatorDuplicated in Integers(1,10),
             for generatorDuplicated in Integers(1,10) ];

structure

    foreach goodIndex in Integers(1,10) do
      goodEntity[ goodIndex ].o --> goodEntity[ goodIndex ].i;
      goodEntity[ badIndex  ].src --> goodEntity[ goodIndex ].dst;
    end

  inPort  --> outPort;
  
end
