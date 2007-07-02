
network Fibs () ==> Out:

entities
  init1 = InitialTokens(tokens = [1]);
  init2 = InitialTokens(tokens = [1]);
  add = Add();

structure
  init1.Out --> init2.In;
  init1.Out --> add.A;
  init2.Out --> add.B;
  add.Result --> init1.In;
  add.Result --> Out;
end
