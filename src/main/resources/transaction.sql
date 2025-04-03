CREATE TABLE public."transaction" (
                                      id uuid NOT NULL,
                                      iscompleted boolean DEFAULT false NOT NULL,
                                      amount double precision NOT NULL,
                                      buyerid varchar NOT NULL,
                                      sellerid varchar NOT NULL,
                                      CONSTRAINT transaction_pk PRIMARY KEY (id)
);

-- Auto-generated SQL script #202501280919
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7831'::uuid,false,10.0,'2','3');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7832'::uuid,true,567.0,'1','4');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7833'::uuid,false,10.0,'5','9');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7834'::uuid,true,54.0,'4','2');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7835'::uuid,true,10.0,'1','5');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7836'::uuid,false,44.0,'1','2');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7837'::uuid,true,4433.0,'1','2');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7838'::uuid,false,10.0,'3','6');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee7839'::uuid,true,10.0,'1','2');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee783a'::uuid,true,88.0,'2','7');
INSERT INTO public."transaction" (id,iscompleted,amount,buyerid,sellerid)
VALUES ('c2d29867-3d0b-d497-9191-18a9d8ee783b'::uuid,true,10.0,'1','2');


